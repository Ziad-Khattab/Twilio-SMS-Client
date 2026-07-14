<script>
  import { onMount, onDestroy } from 'svelte';
  import { Send, Check, CheckCheck, User } from 'lucide-svelte';

  let users = $state([]);
  let messages = $state([]);
  let activeUserId = $state(null);
  let activeUser = $derived(users.find(u => u.id === activeUserId));
  let input = $state('');
  let sending = $state(false);
  let error = $state('');
  let ws = $state(null);
  let connected = $state(false);

  function connectWs() {
    const proto = location.protocol === 'https:' ? 'wss:' : 'ws:';
    ws = new WebSocket(`${proto}//${location.host}/ws/chat`);
    ws.onopen = () => connected = true;
    ws.onclose = () => { connected = false; setTimeout(connectWs, 3000); };
    ws.onmessage = (e) => {
      try {
        const data = JSON.parse(e.data);
        if (data.type === 'new_message' && data.senderId === activeUserId) {
          loadHistory();
        }
      } catch (ignored) {}
    };
  }

  onMount(() => {
    fetchUsers();
    connectWs();
  });

  onDestroy(() => { if (ws) ws.close(); });

  async function fetchUsers() {
    try {
      const res = await fetch('/api/chat/users');
      if (res.ok) { const d = await res.json(); users = d.users || []; }
    } catch (ignored) {}
  }

  async function loadHistory() {
    if (!activeUserId) return;
    try {
      const res = await fetch(`/api/chat/history?with=${activeUserId}&limit=100`);
      if (res.ok) { const d = await res.json(); messages = (d.messages || []).reverse(); }
    } catch (ignored) {}
  }

  $effect(() => {
    if (activeUserId) loadHistory();
  });

  async function handleSend(e) {
    e.preventDefault();
    if (!input.trim() || !activeUserId) return;
    sending = true;
    error = '';
    try {
      const res = await fetch('/api/chat/send', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ recipientId: activeUserId, content: input.trim() })
      });
      if (res.ok) {
        input = '';
        loadHistory();
      } else {
        const d = await res.json();
        error = d.message || 'Send failed';
      }
    } catch (ignored) {
      error = 'Connection error';
    } finally {
      sending = false;
    }
  }

  // Unread polling for badge (fallback if WS not connected)
  let internalUnread = $state(0);
  onMount(() => {
    const iv = setInterval(async () => {
      try {
        const res = await fetch('/api/chat/unread');
        if (res.ok) { const d = await res.json(); internalUnread = d.internalUnread || 0; }
      } catch (ignored) {}
    }, 10000);
    return () => clearInterval(iv);
  });
</script>

<div class="grid grid-cols-1 lg:grid-cols-4 gap-6 flex-grow items-stretch mb-6">
  <!-- Left: User contact list -->
  <div class="lg:col-span-1 card-glass p-0 flex flex-col min-h-[500px]">
    <div class="p-4 border-b border-[var(--border)] bg-white/[0.02]">
      <span class="font-bold text-sm tracking-wide uppercase text-[var(--text-secondary)]">Users</span>
    </div>
    <div class="flex-grow overflow-y-auto max-h-[500px] lg:max-h-[600px]">
      {#if users.length === 0}
        <div class="text-center p-8 text-[var(--text-muted)] text-sm">No other users found.</div>
      {:else}
        {#each users as u}
          <button
            class="w-full contact-item block {activeUserId === u.id ? 'active' : ''}"
            onclick={() => activeUserId = u.id}
          >
            <div class="contact-name">
              <User size={14} class="inline mr-1 opacity-50" />
              {u.fullName || u.username}
            </div>
            <div class="contact-phone truncate text-[var(--text-muted)]">{u.msisdn ? u.msisdn : 'No phone'}</div>
          </button>
        {/each}
      {/if}
    </div>
  </div>

  <!-- Right: Chat panel -->
  <div class="lg:col-span-3 card-glass p-0 flex flex-col min-h-[500px]">
    {#if activeUser}
      <div class="p-4 border-b border-[var(--border)] bg-white/[0.02] flex items-center justify-between">
        <div>
          <span class="text-xs text-[var(--text-secondary)] block uppercase font-bold tracking-wider">Internal Chat with</span>
          <span class="font-bold text-[var(--cyan)]">{activeUser.fullName || activeUser.username}</span>
        </div>
      </div>

      <div class="flex-grow p-6 overflow-y-auto flex flex-col gap-4 max-h-[400px] lg:max-h-[500px] bg-black/20">
        {#if messages.length === 0}
          <div class="empty-state text-sm">Start a conversation with {activeUser.fullName || activeUser.username}.</div>
        {:else}
          {#each messages as msg}
            <div class="message-bubble {msg.senderId === activeUserId ? 'inbound' : 'outbound'}">
              <div class="msg-text">{msg.content}</div>
              <div class="msg-meta">
                <span>{new Date(msg.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                {#if msg.senderId !== activeUserId}
                  <span title="Delivered"><CheckCheck size={14} class="text-[var(--emerald)]" /></span>
                {/if}
              </div>
            </div>
          {/each}
        {/if}
      </div>

      {#if error}
        <div class="error-msg mx-4 my-2 text-xs">{error}</div>
      {/if}

      <form onsubmit={handleSend} class="chat-input-bar">
        <input
          type="text"
          class="input flex-grow"
          placeholder="Type internal message..."
          bind:value={input}
          disabled={sending}
          required
          aria-label="Internal message"
        />
        <button type="submit" class="btn btn-primary px-6" disabled={sending || !input.trim()}>
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
        <User size={40} class="text-[var(--text-muted)] mb-2" />
        <span>Select a user</span>
        <p class="text-sm text-[var(--text-muted)] max-w-xs mt-2">Choose a user from the list to start an internal conversation.</p>
      </div>
    {/if}
  </div>
</div>
