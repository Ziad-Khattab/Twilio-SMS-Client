<script>
  import { LogIn, User, LockKeyhole } from 'lucide-svelte';

  let { onLoginSuccess, navigate } = $props();

  let username = $state('');
  let password = $state('');
  let error = $state('');
  let loading = $state(false);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      error = 'Username and password are required';
      return;
    }

    loading = true;
    error = '';

    try {
      const res = await fetch('/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username.trim(), password })
      });

      const data = await res.json();
      if (res.ok && data.status === 'success') {
        onLoginSuccess(data.role, data.userId);
      } else {
        error = data.message || 'Invalid username or password';
      }
    } catch (err) {
      console.error(err);
      error = 'Unable to connect to the server';
    } finally {
      loading = false;
    }
  }
</script>

<div class="flex items-center justify-center min-h-[70vh] px-4 animate-fade">
  <div class="card-glass w-full max-w-[420px] p-8 md:p-10 text-center">
    <div class="mb-8">
      <div class="logo-container justify-center mb-2">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--cyan)" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="4"/><path d="M8 9h8"/><path d="M8 13h6"/><path d="M8 17h4"/></svg>
        <span>Twilio SMS</span>
        <div class="logo-dot"></div>
      </div>
      <p class="text-sm text-[var(--text-secondary)]">Sign in to your account</p>
    </div>

    {#if error}
      <div class="error-msg mb-4 text-left">
        {error}
      </div>
    {/if}

    <form onsubmit={handleSubmit}>
      <div class="form-group">
        <label class="label" for="username">Username</label>
        <div class="search-input-container">
          <User size={16} class="text-[var(--text-muted)] shrink-0" />
          <input
            id="username"
            type="text"
            class="input"
            placeholder="Enter username"
            bind:value={username}
            required
            disabled={loading}
          />
        </div>
      </div>

      <div class="form-group mb-6">
        <label class="label" for="password">Password</label>
        <div class="search-input-container">
          <LockKeyhole size={16} class="text-[var(--text-muted)] shrink-0" />
          <input
            id="password"
            type="password"
            class="input"
            placeholder="Enter password"
            bind:value={password}
            required
            disabled={loading}
          />
        </div>
      </div>

      <button type="submit" class="btn btn-primary w-full py-3" disabled={loading}>
        {#if loading}
          <svg class="animate-spin h-4 w-4" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>
          Signing in...
        {:else}
          <LogIn size={18} />
          Sign In
        {/if}
      </button>
    </form>

    <div class="mt-6 text-sm text-[var(--text-secondary)]">
      Don't have an account? 
      <button class="text-[var(--cyan)] hover:underline ml-1 font-semibold" onclick={() => navigate('register')}>
        Register
      </button>
    </div>
  </div>
</div>
