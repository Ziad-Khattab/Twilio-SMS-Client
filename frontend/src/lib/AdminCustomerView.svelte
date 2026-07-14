<script>
  let { customer = {}, isOpen = false, onClose, onSave } = $props();

  let loading = $state(false);
  let error = $state('');

  // Local form states
  let username = $state('');
  let password = $state('');
  let fullName = $state('');
  let birthday = $state('');
  let msisdn = $state('');
  let job = $state('');
  let email = $state('');
  let address = $state('');
  let twilioSid = $state('');
  let twilioToken = $state('');
  let twilioSender = $state('');

  let smsProvider = $state('');
  let smppHost = $state('');
  let smppPort = $state('');
  let smppSystemId = $state('');
  let smppPassword = $state('');
  let smppAddressRange = $state('');

  let isEditMode = $derived(!!customer.id);

  // Sync state when customer prop changes or modal opens
  $effect(() => {
    if (isOpen) {
      username = customer.username || '';
      fullName = customer.fullName || '';
      birthday = customer.birthday || '';
      msisdn = customer.msisdn || '';
      job = customer.job || '';
      email = customer.email || '';
      address = customer.address || '';
      twilioSid = customer.twilioSid || '';
      twilioSender = customer.twilioSender || '';
      password = '';
      twilioToken = '';
      smsProvider = customer.smsProvider || '';
      smppHost = customer.smppHost || '';
      smppPort = customer.smppPort || '';
      smppSystemId = customer.smppSystemId || '';
      smppPassword = '';
      smppAddressRange = customer.smppAddressRange || '';
      error = '';
    }
  });

  async function handleSave(e) {
    e.preventDefault();
    if (!username.trim() || !fullName.trim() || !msisdn.trim() || !email.trim()) {
      error = 'Please fill in all required fields';
      return;
    }

    if (!isEditMode && !password) {
      error = 'Password is required for new accounts';
      return;
    }

    if (msisdn.trim() && msisdn.trim().length < 4) {
      error = 'MSISDN must be at least 4 characters';
      return;
    }

    loading = true;
    error = '';

    const payload = {
      actionType: isEditMode ? 'edit' : 'create',
      username: username.trim(),
      fullName: fullName.trim(),
      birthday,
      msisdn: msisdn.trim(),
      job: job.trim(),
      email: email.trim(),
      address: address.trim(),
      twilioSid: twilioSid.trim(),
      twilioSender: twilioSender.trim(),
      smsProvider,
      smppHost: smppHost.trim(),
      smppPort: smppPort.trim(),
      smppSystemId: smppSystemId.trim(),
      smppAddressRange: smppAddressRange.trim()
    };

    if (isEditMode) {
      payload.customerId = customer.id;
    }

    if (password) {
      payload.password = password;
    }
    if (twilioToken) {
      payload.twilioToken = twilioToken;
    }
    if (smppPassword) {
      payload.smppPassword = smppPassword;
    }

    try {
      const res = await fetch('/admin/customer', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await res.json();
      if (res.ok && data.status === 'success') {
        onSave();
        onClose();
      } else {
        error = data.message || 'Operation failed';
      }
    } catch (err) {
      console.error(err);
      error = 'Server connection error';
    } finally {
      loading = false;
    }
  }
</script>

{#if isOpen}
  <!-- svelte-ignore a11y_no_static_element_interactions -->
  <div class="fixed inset-0 bg-black/80 backdrop-blur-sm z-50 flex items-center justify-center p-4 overflow-y-auto" onkeydown={(e) => e.key === 'Escape' && onClose()}>
    <div class="card-glass w-full max-w-[600px] p-6 my-8 animate-fade text-left" role="dialog" aria-modal="true" aria-label={isEditMode ? 'Edit Customer Settings' : 'Create New Customer'}>
      <div class="flex justify-between items-center mb-6 pb-2 border-b border-[var(--border)]">
        <h3 class="font-bold text-lg text-white">
          {isEditMode ? 'Edit Customer Settings' : 'Create New Customer'}
        </h3>
        <button class="text-white/40 hover:text-white" onclick={onClose} disabled={loading}>✕</button>
      </div>

      {#if error}
        <div class="error-msg mb-4">
          {error}
        </div>
      {/if}

      <form onsubmit={handleSave}>
        <h4 class="text-xs font-bold uppercase tracking-wider text-[var(--cyan)] mb-4">Identity Details</h4>
        
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div class="form-group">
            <label class="label" for="adminCustUser">Username *</label>
            <input id="adminCustUser" type="text" class="input" bind:value={username} required disabled={isEditMode || loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustPass">Password {isEditMode ? '(blank to keep current)' : '*'}</label>
            <input id="adminCustPass" type="password" class="input" bind:value={password} required={!isEditMode} disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustName">Full Name *</label>
            <input id="adminCustName" type="text" class="input" bind:value={fullName} required disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustMsisdn">Mobile (MSISDN) *</label>
            <input id="adminCustMsisdn" type="tel" class="input" bind:value={msisdn} required disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustEmail">Email *</label>
            <input id="adminCustEmail" type="email" class="input" bind:value={email} required disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustBday">Birthday</label>
            <input id="adminCustBday" type="date" class="input" bind:value={birthday} disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustJob">Job</label>
            <input id="adminCustJob" type="text" class="input" bind:value={job} disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustAddr">Address</label>
            <input id="adminCustAddr" type="text" class="input" bind:value={address} disabled={loading} />
          </div>
        </div>

        <h4 class="text-xs font-bold uppercase tracking-wider text-[var(--cyan)] mb-4 border-t border-[var(--border)] pt-4">Twilio API configuration</h4>
        
        <div class="grid grid-cols-1 gap-4 mb-6">
          <div class="form-group">
            <label class="label" for="adminCustTwilioSid">Twilio Account SID</label>
            <input id="adminCustTwilioSid" type="text" class="input font-mono" bind:value={twilioSid} placeholder="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustTwilioToken">Twilio Auth Token {isEditMode ? '(blank to keep current)' : '(optional — only if using Twilio SMS)'}</label>
            <input id="adminCustTwilioToken" type="password" class="input font-mono" bind:value={twilioToken} placeholder="Auth Token" disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustTwilioSender">Twilio Sender ID (Phone Number)</label>
            <input id="adminCustTwilioSender" type="text" class="input font-mono" bind:value={twilioSender} placeholder="+1xxxxxxxxxx" disabled={loading} />
          </div>
        </div>

        <h4 class="text-xs font-bold uppercase tracking-wider text-[var(--cyan)] mb-4 border-t border-[var(--border)] pt-4">SMS Provider</h4>

        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div class="form-group">
            <label class="label" for="adminCustSmsProvider">Provider</label>
            <select id="adminCustSmsProvider" class="input" bind:value={smsProvider} disabled={loading}>
              <option value="">Default (Twilio)</option>
              <option value="TWILIO">Twilio</option>
              <option value="SMPP">SMPP</option>
              <option value="AUTO">Auto (SMPP → Twilio)</option>
            </select>
          </div>
          <div class="form-group">
            <label class="label" for="adminCustSmppHost">SMSC Host</label>
            <input id="adminCustSmppHost" type="text" class="input font-mono" bind:value={smppHost} placeholder="e.g. 127.0.0.1" disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustSmppPort">SMSC Port</label>
            <input id="adminCustSmppPort" type="number" class="input font-mono" bind:value={smppPort} placeholder="2776" disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustSmppSystemId">System ID</label>
            <input id="adminCustSmppSystemId" type="text" class="input font-mono" bind:value={smppSystemId} disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustSmppPassword">SMPP Password {isEditMode ? '(blank to keep current)' : ''}</label>
            <input id="adminCustSmppPassword" type="password" class="input font-mono" bind:value={smppPassword} disabled={loading} />
          </div>
          <div class="form-group">
            <label class="label" for="adminCustSmppAddrRange">Address Range (Sender ID)</label>
            <input id="adminCustSmppAddrRange" type="text" class="input font-mono" bind:value={smppAddressRange} disabled={loading} />
          </div>
        </div>

        <div class="flex gap-3 justify-end border-t border-[var(--border)] pt-4">
          <button type="button" class="btn btn-secondary" onclick={onClose} disabled={loading}>
            Cancel
          </button>
          <button type="submit" class="btn btn-primary px-6" disabled={loading}>
            {#if loading}
              Saving Changes...
            {:else}
              Save Customer
            {/if}
          </button>
        </div>
      </form>
    </div>
  </div>
{/if}
