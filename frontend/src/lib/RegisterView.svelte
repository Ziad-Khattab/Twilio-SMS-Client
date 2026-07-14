<script>
  import { UserPlus, ArrowLeft, ArrowRight, Smartphone, Shield } from 'lucide-svelte';
  let { navigate } = $props();

  let step = $state(1); // 1 = Profile details, 2 = Twilio config
  let error = $state('');
  let loading = $state(false);

  // Form states
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

  function nextStep() {
    // Basic validation for Step 1
    if (!username.trim() || !password.trim() || !fullName.trim() || !msisdn.trim() || !email.trim()) {
      error = 'Please fill out all required fields marked with *';
      return;
    }
    error = '';
    step = 2;
  }

  function prevStep() {
    error = '';
    step = 1;
  }

  async function handleSubmit(e) {
    e.preventDefault();

    loading = true;
    error = '';

    try {
      const res = await fetch('/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          username: username.trim(),
          password,
          fullName: fullName.trim(),
          birthday,
          msisdn: msisdn.trim(),
          job: job.trim(),
          email: email.trim(),
          address: address.trim(),
          twilioSid: twilioSid.trim(),
          twilioToken,
          twilioSender: twilioSender.trim()
        })
      });

      const data = await res.json();
      if (res.ok && data.status === 'success') {
        navigate('verify-msisdn');
      } else {
        error = data.message || 'Registration failed. Check inputs.';
      }
    } catch (err) {
      console.error(err);
      error = 'Unable to connect to server';
    } finally {
      loading = false;
    }
  }
</script>

<div class="flex items-center justify-center min-h-[80vh] px-4 py-8 animate-fade">
  <div class="card-glass w-full max-w-[540px] p-8 md:p-10">
    <div class="text-center mb-8">
      <div class="logo-container justify-center mb-2">
        <UserPlus size={22} class="text-[var(--cyan)]" />
        <span>Twilio SMS</span>
        <div class="logo-dot"></div>
      </div>
      <p class="text-sm text-[var(--text-secondary)]">Create your customer account</p>
      
      <!-- Stepper indicator -->
      <div class="flex justify-center items-center gap-2 mt-4">
        <div class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold {step === 1 ? 'bg-[var(--cyan)] text-black' : 'bg-white/10 text-white'}">1</div>
        <div class="w-12 h-[1px] bg-white/20"></div>
        <div class="w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold {step === 2 ? 'bg-[var(--cyan)] text-black' : 'bg-white/10 text-white'}">2</div>
      </div>
    </div>

    {#if error}
      <div class="error-msg mb-4">
        {error}
      </div>
    {/if}

    {#if step === 1}
      <form onsubmit={(e) => { e.preventDefault(); nextStep(); }}>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div class="form-group">
            <label class="label" for="username">Username *</label>
            <input id="username" type="text" class="input" bind:value={username} required placeholder="Choose a username" />
          </div>
          
          <div class="form-group">
            <label class="label" for="password">Password *</label>
            <input id="password" type="password" class="input" bind:value={password} required placeholder="Create a password" />
          </div>

          <div class="form-group">
            <label class="label" for="fullName">Full Name *</label>
            <input id="fullName" type="text" class="input" bind:value={fullName} required placeholder="Your full name" />
          </div>

          <div class="form-group">
            <label class="label" for="birthday">Birthday</label>
            <input id="birthday" type="date" class="input" bind:value={birthday} />
          </div>

          <div class="form-group">
            <label class="label" for="msisdn">Mobile (MSISDN) *</label>
            <input id="msisdn" type="tel" class="input" bind:value={msisdn} required placeholder="+1234567890" />
          </div>

          <div class="form-group">
            <label class="label" for="email">Email *</label>
            <input id="email" type="email" class="input" bind:value={email} required placeholder="your@email.com" />
          </div>

          <div class="form-group">
            <label class="label" for="job">Job Title</label>
            <input id="job" type="text" class="input" bind:value={job} placeholder="e.g. Consultant" />
          </div>

          <div class="form-group">
            <label class="label" for="address">Address</label>
            <input id="address" type="text" class="input" bind:value={address} placeholder="Street, City, Country" />
          </div>
        </div>

        <button type="submit" class="btn btn-primary w-full py-3 mt-6">
          Next Step: Twilio Setup
          <ArrowRight size={16} />
        </button>
      </form>
    {:else}
      <form onsubmit={handleSubmit}>
        <div class="form-group">
          <label class="label" for="twilioSid">Twilio Account SID</label>
          <input
            id="twilioSid"
            type="text"
            class="input font-mono"
            bind:value={twilioSid}
            placeholder="ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
            disabled={loading}
          />
        </div>

        <div class="form-group">
          <label class="label" for="twilioToken">Twilio Auth Token</label>
          <input
            id="twilioToken"
            type="password"
            class="input font-mono"
            bind:value={twilioToken}
            required
            placeholder="Your Auth Token"
            disabled={loading}
          />
        </div>

        <div class="form-group mb-6">
          <label class="label" for="twilioSender">Twilio Sender ID (Phone Number)</label>
          <input
            id="twilioSender"
            type="text"
            class="input font-mono"
            bind:value={twilioSender}
            placeholder="+1xxxxxxxxxx"
            disabled={loading}
          />
          <span class="text-xs text-[var(--text-muted)] mt-1 block">Your Twilio phone number used as the SMS sender ID.</span>
        </div>

        <div class="flex gap-4 mt-6">
          <button type="button" class="btn btn-secondary flex-1 py-3" onclick={prevStep} disabled={loading}>
            <ArrowLeft size={16} />
            Back
          </button>
          <button type="submit" class="btn btn-primary flex-1 py-3" disabled={loading}>
            {#if loading}
              <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>
              Sending OTP...
            {:else}
              <Smartphone size={16} />
              Verify Mobile
            {/if}
          </button>
        </div>
      </form>
    {/if}

    <div class="mt-6 text-center text-sm text-[var(--text-secondary)]">
      Already have an account? 
      <button class="text-[var(--cyan)] hover:underline ml-1 font-semibold" onclick={() => navigate('login')}>
        Sign In
      </button>
    </div>
  </div>
</div>
