<script>
  import { onMount } from 'svelte';
  import LoginView from './lib/LoginView.svelte';
  import RegisterView from './lib/RegisterView.svelte';
  import VerifyMsisdnView from './lib/VerifyMsisdnView.svelte';
  import CustomerDashboard from './lib/CustomerDashboard.svelte';
  import AdminDashboard from './lib/AdminDashboard.svelte';

  let currentView = $state('login');
  let userRole = $state('');
  let userId = $state(null);
  let verifyingSession = $state(true);

  function getViewFromPath(path) {
    if (path === '/') return 'login';
    if (path === '/register') return 'register';
    if (path === '/verify') return 'verify-msisdn';
    if (path === '/chat') return 'customer-dashboard';
    if (path === '/admin/' || path === '/admin') return 'admin-dashboard';
    return null;
  }

  function syncUrl() {
    const pathMap = {
      'login': '/',
      'register': '/register',
      'verify-msisdn': '/verify',
      'customer-dashboard': '/chat',
      'admin-dashboard': '/admin/'
    };
    const url = pathMap[currentView] || '/login';
    if (window.location.pathname !== url) {
      history.pushState({ view: currentView }, '', url);
    }
  }

  async function checkSession() {
    try {
      const res = await fetch('/profile');
      if (res.ok) {
        const data = await res.json();
        userRole = data.role || '';
        currentView = userRole === 'administrator' ? 'admin-dashboard' : 'customer-dashboard';
      } else {
        currentView = 'login';
      }
    } catch (err) {
      console.error(err);
      currentView = 'login';
    } finally {
      verifyingSession = false;
      syncUrl();
    }
  }

  onMount(() => {
    window.addEventListener('popstate', () => {
      const path = window.location.pathname;
      const viewFromPath = getViewFromPath(path);
      if (viewFromPath) {
        currentView = viewFromPath;
      } else {
        checkSession();
      }
    });
    checkSession();
  });

  function handleLoginSuccess(role, id) {
    userRole = role;
    userId = id;
    currentView = role === 'administrator' ? 'admin-dashboard' : 'customer-dashboard';
    syncUrl();
  }

  function handleLogout() {
    userRole = '';
    userId = null;
    currentView = 'login';
    syncUrl();
  }

  function navigate(view) {
    currentView = view;
    syncUrl();
  }
</script>

<main class="app-canvas min-h-screen">
  {#if verifyingSession}
    <div class="flex items-center justify-center min-h-screen">
      <div class="text-white font-bold animate-pulse">Verifying secure session...</div>
    </div>
  {:else}
    {#if currentView === 'login'}
      <LoginView onLoginSuccess={handleLoginSuccess} {navigate} />
    {:else if currentView === 'register'}
      <RegisterView {navigate} />
    {:else if currentView === 'verify-msisdn'}
      <VerifyMsisdnView {navigate} />
    {:else if currentView === 'customer-dashboard'}
      <CustomerDashboard onLogout={handleLogout} />
    {:else if currentView === 'admin-dashboard'}
      <AdminDashboard onLogout={handleLogout} />
    {/if}
  {/if}
</main>
