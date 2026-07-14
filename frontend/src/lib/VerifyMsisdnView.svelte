<script>
  let { navigate } = $props();

  let code = $state('');
  let error = $state('');
  let message = $state('');
  let loading = $state(false);
  let resending = $state(false);

  async function handleVerify(e) {
    e.preventDefault();
    if (!/^\d{6}$/.test(code.trim())) {
      error = 'Please enter the 6-digit verification code';
      return;
    }

    loading = true;
    error = '';
    message = '';

    try {
      const res = await fetch('/verify-msisdn', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ code: code.trim() })
      });

      if (res.ok) {
        message = 'Verification successful! Redirecting to login...';
        setTimeout(() => {
          navigate('login');
        }, 2000);
      } else {
        const data = await res.json();
        error = data.message || 'Invalid verification code';
      }
    } catch (err) {
      console.error(err);
      error = 'Unable to connect to server';
    } finally {
      loading = false;
    }
  }

  async function handleResend() {
    resending = true;
    error = '';
    message = '';

    try {
      const res = await fetch('/verify-msisdn?action=resend', {
        method: 'POST'
      });

      const data = await res.json();
      if (res.ok && data.status === 'success') {
        message = 'A new verification code has been sent to your phone.';
      } else {
        error = data.message || 'Failed to resend verification code';
      }
    } catch (err) {
      console.error(err);
      error = 'Unable to connect to server';
    } finally {
      resending = false;
    }
  }
</script>

<div class="flex items-center justify-center min-h-[70vh] px-4 animate-fade">
  <div class="card-glass w-full max-w-[420px] p-8 md:p-10 text-center">
    <div class="mb-8">
      <div class="logo-container justify-center mb-2">
        <span>Verify Mobile</span>
        <div class="logo-dot"></div>
      </div>
      <p class="text-sm text-[var(--text-secondary)]">We sent a 6-digit PIN to verify your number.</p>
    </div>

    {#if error}
      <div class="error-msg mb-4 text-left">
        {error}
      </div>
    {/if}

    {#if message}
      <div class="bg-emerald-500/10 border border-emerald-500/20 text-[var(--emerald)] p-3 rounded mb-4 text-sm text-left">
        {message}
      </div>
    {/if}

    <form onsubmit={handleVerify}>
      <div class="form-group mb-6">
        <label class="label text-center" for="code">Verification Code</label>
        <input
          id="code"
          type="text"
          class="input text-center text-2xl tracking-[0.4em] font-mono py-3"
          maxlength="6"
          placeholder="000000"
          bind:value={code}
          required
          disabled={loading || resending}
        />
      </div>

      <button type="submit" class="btn btn-primary w-full py-3 mb-4" disabled={loading || resending}>
        {#if loading}
          Verifying...
        {:else}
          Confirm Verification
        {/if}
      </button>

      <button
        type="button"
        class="btn btn-secondary w-full py-3"
        onclick={handleResend}
        disabled={loading || resending}
      >
        {#if resending}
          Resending...
        {:else}
          Resend Verification Code
        {/if}
      </button>
    </form>

    <div class="mt-6 text-sm text-[var(--text-secondary)]">
      Need to change details? 
      <button class="text-[var(--cyan)] hover:underline ml-1 font-semibold" onclick={() => navigate('register')} disabled={loading}>
        Start Over
      </button>
    </div>
  </div>
</div>
