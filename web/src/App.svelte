<script lang="ts">
    import Counter from './lib/Counter.svelte'
    import Chat from "./lib/Chat.svelte";

</script>

<main>

    <h1 class="text-3xl font-bold underline">
        Hello world!
    </h1>

    <div class="card">
        <Counter/>
    </div>

    <div class="card">
        <Chat/>
    </div>

</main>

