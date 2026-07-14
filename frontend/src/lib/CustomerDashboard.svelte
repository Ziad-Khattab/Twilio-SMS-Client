<script>
  import { onMount } from 'svelte';
  import { MessageSquare, Plus, Trash2, Check, CheckCheck, X, Send, LogOut, User, Settings, Phone, Users } from 'lucide-svelte';
  import InternalChat from './InternalChat.svelte';
  import SystemConversation from './SystemConversation.svelte';

  let { onLogout } = $props();

  let loading = $state(true);
  let error = $state('');
  
  // Data from server
  let profile = $state({});
  let outboundHistory = $state([]);
  let inboundHistory = $state([]);
  
  // UI States
  let activeContact = $state('');
  let newMessage = $state('');
  let sendError = $state('');
  let sending = $state(false);
  let newContactNum = $state('');
  let isNewChatModalOpen = $state(false);
  
  // Profile Modal State
  let isProfileModalOpen = $state(false);
  let profileEdit = $state({});
  let profileError = $state('');
  let profileSuccess = $state('');
  let profileSaving = $state(false);

  // Tab state
  let chatMode = $state('sms');

  // Grouped conversations
  // Key: phone number, Value: array of messages sorted by sentAt
  let threads = $derived.by(() => {
    const list = {};
    const myNum = PhoneNormalize(profile.twilioSender || '');
    
    // Process outbound messages
    outboundHistory.forEach(msg => {
      const contact = PhoneNormalize(msg.recipient || '');
      if (!contact) return;
      if (!list[contact]) list[contact] = [];
      list[contact].push({
        id: msg.id,
        direction: 'outbound',
        from: msg.from,
        to: msg.recipient,
        message: msg.message,
        status: msg.status,
        sentAt: msg.sentAt
      });
    });

    // Process inbound messages
    inboundHistory.forEach(msg => {
      const contact = PhoneNormalize(msg.from || '');
      if (!contact) return;
      if (!list[contact]) list[contact] = [];
      list[contact].push({
        id: msg.id,
        direction: 'inbound',
        from: msg.from,
        to: msg.recipient,
        message: msg.message,
        status: 'delivered', // Inbound is already delivered
        sentAt: msg.sentAt
      });
    });

    // Sort messages in each thread by time ascending
    Object.keys(list).forEach(key => {
      list[key].sort((a, b) => new Date(a.sentAt) - new Date(b.sentAt));
    });

    return list;
  });

  // Derived list of contacts sorted by the time of their latest message
  let contactList = $derived.by(() => {
    return Object.keys(threads).map(phone => {
      const msgs = threads[phone];
      const lastMsg = msgs[msgs.length - 1];
      return {
        phone,
        lastMessage: lastMsg ? lastMsg.message : '',
        lastTime: lastMsg ? lastMsg.sentAt : ''
      };
    }).sort((a, b) => new Date(b.lastTime) - new Date(a.lastTime));
  });

  function PhoneNormalize(phone) {
    if (!phone) return '';
    let val = phone.replace(/\s+/g, '');
    if (!val.startsWith('+')) {
      val = '+' + val;
    }
    return val;
  }

  async function fetchDashboard() {
    try {
      const res = await fetch('/dashboard');
      if (res.ok) {
        const data = await res.json();
        profile = data.profile || {};
        outboundHistory = data.outboundHistory || [];
        inboundHistory = data.inboundHistory || [];
        
        // Auto-select first contact if activeContact is empty
        if (!activeContact && Object.keys(threads).length > 0) {
          activeContact = Object.keys(threads)[0];
        }
      } else {
        error = 'Failed to load dashboard data. Session expired?';
        onLogout();
      }
    } catch (err) {
      console.error(err);
      error = 'Server connection error';
    } finally {
      loading = false;
    }
  }

  async function pollDashboard() {
    await fetchDashboard();
    pollTimer = setTimeout(pollDashboard, 5000);
  }

  let pollTimer;

  onMount(() => {
    fetchDashboard().then(() => {
      pollTimer = setTimeout(pollDashboard, 5000);
    });
    return () => clearTimeout(pollTimer);
  });

  async function handleSend(e) {
    if (e) e.preventDefault();
    if (!newMessage.trim() || !activeContact) return;

    sending = true;
    sendError = '';

    try {
      const res = await fetch('/send-sms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          recipient: activeContact,
          message: newMessage.trim()
        })
      });

      const data = await res.json();
      if (res.ok && data.status === 'success') {
        newMessage = '';
        fetchDashboard();
      } else {
        sendError = data.message || 'Failed to dispatch SMS';
      }
    } catch (err) {
      sendError = 'Server connection failure';
    } finally {
      sending = false;
    }
  }

  async function handleDeleteMessage(smsId, direction) {
    if (!confirm('Are you sure you want to delete this message record?')) return;

    try {
      // NOTE: Webhook/inbound messages can be deleted too if deleteSmsByIdAndUserId allows it
      const res = await fetch('/delete-sms', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ smsId })
      });

      if (res.ok) {
        if (direction === 'outbound') {
          outboundHistory = outboundHistory.filter(m => m.id !== smsId);
        } else {
          inboundHistory = inboundHistory.filter(m => m.id !== smsId);
        }
      } else {
        alert('Failed to delete message record');
      }
    } catch (err) {
      console.error(err);
    }
  }

  function startNewChat(e) {
    e.preventDefault();
    const cleanNum = PhoneNormalize(newContactNum);
    if (!cleanNum || cleanNum.length < 5) {
      alert('Please enter a valid recipient phone number');
      return;
    }
    
    activeContact = cleanNum;
    newContactNum = '';
    isNewChatModalOpen = false;
  }

  function openProfileModal() {
    profileEdit = { ...profile, password: '', twilioToken: '' };
    profileError = '';
    profileSuccess = '';
    isProfileModalOpen = true;
  }

  async function handleProfileSave(e) {
    e.preventDefault();
    profileSaving = true;
    profileError = '';
    profileSuccess = '';

    try {
      const res = await fetch('/profile', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(profileEdit)
      });

      if (res.ok) {
        profileSuccess = 'Profile successfully updated!';
        fetchDashboard();
        setTimeout(() => {
          isProfileModalOpen = false;
        }, 1500);
      } else {
        const data = await res.json();
        profileError = data.message || 'Failed to update profile settings';
      }
    } catch (err) {
      profileError = 'Server communication error';
    } finally {
      profileSaving = false;
    }
  }

  async function handleLogout() {
    try {
      await fetch('/logout');
    } catch (err) {
      console.error(err);
    }
    onLogout();
  }
</script>

<div class="app-canvas min-h-screen">
  <div class="container flex-grow flex flex-col">
    <!-- Header -->
    <header class="nav-bar">
      <div class="logo-container">
        <MessageSquare size={22} class="text-[var(--cyan)]" />
        <span>Twilio Messaging</span>
        <div class="logo-dot"></div>
      </div>
      
      <div class="flex items-center gap-3">
        <span class="text-sm text-[var(--text-secondary)] hidden md:inline">
          <User size={14} class="inline mr-1" />
          <strong class="text-white">{profile.fullName || profile.username}</strong>
        </span>
        <button class="btn btn-secondary" onclick={openProfileModal}>
          <Settings size={14} />
          Edit Profile
        </button>
        <button class="btn btn-danger" onclick={handleLogout}>
          <LogOut size={14} />
          Logout
        </button>
      </div>
    </header>

    {#if loading}
      <div class="empty-state">
        <div class="animate-pulse">Loading workspace dashboard...</div>
      </div>
    {:else if error}
      <div class="error-msg my-8 max-w-md mx-auto">
        {error}
      </div>
    {:else}
      <!-- Tab Bar -->
      <div class="flex gap-1 mb-4 bg-white/[0.03] rounded-lg p-1 self-start">
        <button class="tab-btn {chatMode === 'sms' ? 'tab-active' : ''}" onclick={() => chatMode = 'sms'}>
          <Phone size={14} />
          SMS
        </button>
        <button class="tab-btn {chatMode === 'internal' ? 'tab-active' : ''}" onclick={() => chatMode = 'internal'}>
          <Users size={14} />
          Internal
        </button>
        <button class="tab-btn {chatMode === 'system' ? 'tab-active' : ''}" onclick={() => chatMode = 'system'}>
          <MessageSquare size={14} />
          System
        </button>
      </div>

      {#if chatMode === 'sms'}
      <div class="grid grid-cols-1 lg:grid-cols-4 gap-6 flex-grow items-stretch mb-6">
        
        <!-- Left Panel: Chat List / Contacts -->
        <div class="lg:col-span-1 card-glass p-0 flex flex-col min-h-[500px]">
          <div class="p-4 border-b border-[var(--border)] flex justify-between items-center bg-white/[0.02]">
            <span class="font-bold text-sm tracking-wide uppercase text-[var(--text-secondary)]">Conversations</span>
            <button class="btn btn-primary p-2 text-xs" onclick={() => isNewChatModalOpen = true}>
              <Plus size={14} />
              New Chat
            </button>
          </div>

          <div class="flex-grow overflow-y-auto max-h-[500px] lg:max-h-[600px]">
            {#if contactList.length === 0}
              <div class="text-center p-8 text-[var(--text-muted)] text-sm">
                No active threads. Click "New Chat" to start.
              </div>
            {:else}
              {#each contactList as contact}
                <button
                  class="w-full contact-item block {activeContact === contact.phone ? 'active' : ''}"
                  onclick={() => activeContact = contact.phone}
                >
                  <div class="contact-name">{contact.phone}</div>
                  <div class="contact-phone truncate text-[var(--text-muted)]">{contact.lastMessage}</div>
                </button>
              {/each}
            {/if}
          </div>
        </div>

        <!-- Right Panel: Message Feed -->
        <div class="lg:col-span-3 card-glass p-0 flex flex-col min-h-[500px]">
          {#if activeContact}
            <!-- Thread Title -->
            <div class="p-4 border-b border-[var(--border)] bg-white/[0.02] flex items-center justify-between">
              <div>
                <span class="text-xs text-[var(--text-secondary)] block uppercase font-bold tracking-wider">Conversation with</span>
                <span class="font-mono text-[var(--cyan)] font-bold text-lg">{activeContact}</span>
              </div>
            </div>

            <!-- Messages Stream -->
            <div class="flex-grow p-6 overflow-y-auto flex flex-col gap-4 max-h-[400px] lg:max-h-[500px] bg-black/20">
              {#if !threads[activeContact] || threads[activeContact].length === 0}
                <div class="empty-state text-sm">
                  This is the start of your message thread with {activeContact}.
                </div>
              {:else}
                {#each threads[activeContact] as msg}
                  <div class="message-bubble {msg.direction}">
                    <div class="msg-text">{msg.message}</div>
                    
                    <div class="msg-meta">
                      <span>{new Date(msg.sentAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                      {#if msg.direction === 'outbound'}
                        {#if msg.status === 'delivered'}
                          <span class="text-[var(--emerald)]" title="Delivered"><CheckCheck size={14} /></span>
                        {:else if msg.status === 'failed'}
                          <span class="text-[var(--red)]" title="Failed"><X size={14} /></span>
                        {:else}
                          <span class="text-[var(--cyan)]" title="Pending"><Check size={14} /></span>
                        {/if}
                      {/if}
                      <button
                        class="ml-2 hover:text-[var(--red)] transition-colors opacity-40 hover:opacity-100"
                        onclick={() => handleDeleteMessage(msg.id, msg.direction)}
                        title="Delete log"
                      >
                        <Trash2 size={12} />
                      </button>
                    </div>
                  </div>
                {/each}
              {/if}
            </div>

            <!-- Error message if send fails -->
            {#if sendError}
              <div class="error-msg mx-4 my-2 text-xs">
                {sendError}
              </div>
            {/if}

            <!-- Chat Input Bar -->
            <form onsubmit={handleSend} class="chat-input-bar">
              <input
                type="text"
                class="input flex-grow"
                placeholder="Type your message here..."
                bind:value={newMessage}
                disabled={sending}
                required
                aria-label="Type your message"
              />
              <button type="submit" class="btn btn-primary px-6" disabled={sending || !newMessage.trim()}>
                {#if sending}
                  <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>
                  Sending...
                {:else}
                  <Send size={16} />
                  Send
                {/if}
              </button>
            </form>
          {:else}
            <div class="empty-state">
              <MessageSquare size={40} class="text-[var(--text-muted)] mb-2" />
              <span>No chat selected</span>
              <p class="text-sm text-[var(--text-muted)] max-w-xs mt-2">
                Select an existing conversation thread from the sidebar or click "New Chat" to dispatch a message.
              </p>
            </div>
          {/if}
        </div>

      </div> <!-- end sms grid -->

      {/if}
      {#if chatMode === 'internal'}
        <InternalChat />
      {/if}
      {#if chatMode === 'system'}
        <SystemConversation />
      {/if}
    {/if}
  </div>
</div>

<!-- Modal: New Chat -->
{#if isNewChatModalOpen}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4" onkeydown={(e) => e.key === 'Escape' && (isNewChatModalOpen = false)}>
    <div class="card-glass w-full max-w-[400px] p-6 animate-fade" role="dialog" aria-modal="true" aria-labelledby="new-chat-title">
      <div class="flex justify-between items-center mb-6">
        <h3 id="new-chat-title" class="font-bold text-lg text-white">Start Conversation</h3>
        <button class="text-white/40 hover:text-white" onclick={() => isNewChatModalOpen = false}>✕</button>
      </div>

      <form onsubmit={startNewChat}>
        <div class="form-group mb-6">
          <label class="label" for="newPhone">Recipient Phone Number</label>
          <div class="search-input-container">
            <Phone size={16} class="text-[var(--text-muted)] shrink-0" />
            <input
              id="newPhone"
              type="tel"
              class="input"
              placeholder="+1234567890"
              bind:value={newContactNum}
              required
              autofocus
            />
          </div>
          <span class="text-xs text-[var(--text-muted)] mt-1 block">Include country code (e.g. +1).</span>
        </div>

        <div class="flex gap-3">
          <button type="button" class="btn btn-secondary flex-1" onclick={() => isNewChatModalOpen = false}>
            Cancel
          </button>
          <button type="submit" class="btn btn-primary flex-1">
            Open Chat
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}

<!-- Modal: Edit Profile -->
{#if isProfileModalOpen}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto" onkeydown={(e) => e.key === 'Escape' && (isProfileModalOpen = false)}>
    <div class="card-glass w-full max-w-[600px] p-6 my-8 animate-fade text-left" role="dialog" aria-modal="true" aria-labelledby="profile-title">
      <div class="flex justify-between items-center mb-6 pb-2 border-b border-[var(--border)]">
        <h3 id="profile-title" class="font-bold text-lg text-white">Profile Configurations</h3>
        <button class="text-white/40 hover:text-white" onclick={() => isProfileModalOpen = false}>✕</button>
      </div>

      {#if profileError}
        <div class="error-msg mb-4">
          {profileError}
        </div>
      {/if}
      {#if profileSuccess}
        <div class="bg-emerald-500/10 border border-emerald-500/20 text-[var(--emerald)] p-3 rounded mb-4 text-sm">
          {profileSuccess}
        </div>
      {/if}

      <form onsubmit={handleProfileSave}>
        <h4 class="text-xs font-bold uppercase tracking-wider text-[var(--cyan)] mb-4">Account Information</h4>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div class="form-group">
            <label class="label" for="profileName">Full Name</label>
            <input id="profileName" type="text" class="input" bind:value={profileEdit.fullName} required />
          </div>
          <div class="form-group">
            <label class="label" for="profileMsisdn">Mobile (MSISDN)</label>
            <input id="profileMsisdn" type="tel" class="input" bind:value={profileEdit.msisdn} required />
          </div>
          <div class="form-group">
            <label class="label" for="profileEmail">Email</label>
            <input id="profileEmail" type="email" class="input" bind:value={profileEdit.email} required />
          </div>
          <div class="form-group">
            <label class="label" for="profileBirthday">Birthday</label>
            <input id="profileBirthday" type="date" class="input" bind:value={profileEdit.birthday} />
          </div>
          <div class="form-group">
            <label class="label" for="profileJob">Job</label>
            <input id="profileJob" type="text" class="input" bind:value={profileEdit.job} />
          </div>
          <div class="form-group">
            <label class="label" for="profileAddress">Address</label>
            <input id="profileAddress" type="text" class="input" bind:value={profileEdit.address} />
          </div>
          <div class="form-group col-span-1 md:col-span-2">
            <label class="label" for="profilePass">Change Password (leave empty to keep current)</label>
            <input id="profilePass" type="password" class="input" bind:value={profileEdit.password} placeholder="New password" />
          </div>
        </div>

        <h4 class="text-xs font-bold uppercase tracking-wider text-[var(--cyan)] mb-4 border-t border-[var(--border)] pt-4">Twilio Settings</h4>
        
        <div class="grid grid-cols-1 gap-4 mb-6">
          <div class="form-group">
            <label class="label" for="profileTwilioSid">Twilio Account SID</label>
            <input id="profileTwilioSid" type="text" class="input font-mono" bind:value={profileEdit.twilioSid} />
          </div>
          <div class="form-group">
            <label class="label" for="profileTwilioToken">Twilio Auth Token (leave empty to keep current)</label>
            <input id="profileTwilioToken" type="password" class="input font-mono" bind:value={profileEdit.twilioToken} placeholder="New Twilio Token" />
          </div>
          <div class="form-group">
            <label class="label" for="profileTwilioSender">Twilio Sender ID (Phone Number)</label>
            <input id="profileTwilioSender" type="text" class="input font-mono" bind:value={profileEdit.twilioSender} />
          </div>
        </div>

        <h4 class="text-xs font-bold uppercase tracking-wider text-[var(--cyan)] mb-4 border-t border-[var(--border)] pt-4">SMS Provider</h4>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div class="form-group">
            <label class="label" for="profileSmsProvider">Provider</label>
            <select id="profileSmsProvider" class="input" bind:value={profileEdit.smsProvider}>
              <option value="TWILIO">Twilio</option>
              <option value="SMPP">SMPP</option>
              <option value="AUTO">Auto (SMPP → Twilio)</option>
            </select>
          </div>
          <div class="form-group">
            <label class="label" for="profileSmppHost">SMSC Host</label>
            <input id="profileSmppHost" type="text" class="input font-mono" bind:value={profileEdit.smppHost} placeholder="e.g. 127.0.0.1" />
          </div>
          <div class="form-group">
            <label class="label" for="profileSmppPort">SMSC Port</label>
            <input id="profileSmppPort" type="number" class="input font-mono" bind:value={profileEdit.smppPort} placeholder="2776" />
          </div>
          <div class="form-group">
            <label class="label" for="profileSmppSystemId">System ID</label>
            <input id="profileSmppSystemId" type="text" class="input font-mono" bind:value={profileEdit.smppSystemId} />
          </div>
          <div class="form-group">
            <label class="label" for="profileSmppPassword">Password</label>
            <input id="profileSmppPassword" type="password" class="input font-mono" bind:value={profileEdit.smppPassword} placeholder="Leave empty to keep current" />
          </div>
          <div class="form-group">
            <label class="label" for="profileSmppAddrRange">Address Range (Sender ID)</label>
            <input id="profileSmppAddrRange" type="text" class="input font-mono" bind:value={profileEdit.smppAddressRange} />
          </div>
        </div>

        <div class="flex gap-3 justify-end border-t border-[var(--border)] pt-4">
          <button type="button" class="btn btn-secondary" onclick={() => isProfileModalOpen = false} disabled={profileSaving}>
            Cancel
          </button>
          <button type="submit" class="btn btn-primary px-6" disabled={profileSaving}>
            {#if profileSaving}
              Saving Changes...
            {:else}
              Save Settings
            {/if}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}
