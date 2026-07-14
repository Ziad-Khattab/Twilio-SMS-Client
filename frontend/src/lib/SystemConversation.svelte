<script>
  import { onMount } from 'svelte';
  import { MessageSquare, CheckCheck } from 'lucide-svelte';

  let messages = $state([]);

  onMount(async () => {
    await loadSystem();
    const iv = setInterval(loadSystem, 30000);
    return () => clearInterval(iv);
  });

  async function loadSystem() {
    try {
      const res = await fetch('/api/chat/system?limit=100');
      if (res.ok) {
        const d = await res.json();
        messages = (d.messages || []).reverse();
      }
    } catch (ignored) {}
  }
</script>

<div class="lg:col-span-3 card-glass p-0 flex flex-col min-h-[400px]">
  <div class="p-4 border-b border-[var(--border)] bg-white/[0.02]">
    <span class="text-xs text-[var(--text-secondary)] block uppercase font-bold tracking-wider">System Announcements</span>
  </div>

  <div class="flex-grow p-6 overflow-y-auto flex flex-col gap-4 max-h-[500px] lg:max-h-[600px] bg-black/20">
    {#if messages.length === 0}
      <div class="empty-state text-sm">No system announcements yet.</div>
    {:else}
      {#each messages as msg}
        <div class="message-bubble inbound">
          <div class="msg-text"><span class="font-bold text-[var(--yellow)]">📢 System:</span> {msg.content}</div>
          <div class="msg-meta">
            <span>{new Date(msg.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
            {#if msg.read}
              <span title="Read"><CheckCheck size={14} class="text-[var(--emerald)]" /></span>
            {/if}
          </div>
        </div>
      {/each}
    {/if}
  </div>
</div>
