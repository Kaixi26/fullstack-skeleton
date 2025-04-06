<script lang="ts">

    import { onMount } from 'svelte';

    type Message = { user: string, message: string };
    type GetMessagesResponse = { messages: Message[] }

    async function getChat(): Promise<GetMessagesResponse> {
        const response = await fetch("/api/chat/");
        return response.json();
    }

    async function postMessage(name: string, message: string): Promise<GetMessagesResponse> {
        const response = await fetch("/api/chat/message", {
            method: "POST",
            body: JSON.stringify({user: name, message}),
            headers: {"Content-Type": "application/json"},
        });
        return response.json();
    }


    let myName = $state("Anonymous");
    let myMessage = $state("");
    let chat: Promise<GetMessagesResponse> = $state(getChat());

    onMount(() => {
        const interval = setInterval(() => {
            getChat().then(response => {
                chat = Promise.resolve(response);
            })
        }, 10000);

        return () => clearInterval(interval);
    });

    const handleSendMessage = (e: SubmitEvent) => {
        e.preventDefault();
        postMessage(myName, myMessage).then(response => {
            chat = Promise.resolve(response);
        })
        myMessage = "";
    }

</script>

<div class="chat-window">
    <div class="messages-window">
        {#await chat}
            Fetching chat ...
        {:then chat}
            {#each chat.messages.toReversed() as message}
                <span class="message">{message.user}: {message.message}</span>
            {/each}
        {:catch _}
            Failed getting counter ...
        {/await}
    </div>
    <form onsubmit={handleSendMessage}>
        <input type="text" bind:value={myName}/>
        <input placeholder="Type message..." type="text" bind:value={myMessage}/>
        <button type="submit">
            Submit
        </button>
    </form>
</div>

<style>
    .chat-window {
        background: #AAAAAA;
        flex: 1;
        border-radius: 10px;
        flex-direction: column;
        padding: 10px;
    }

    .messages-window {
        width: 400px;
        height: 500px;
        border-radius: 10px;
        background: #BBBBBB;
        margin-bottom: 10px;
        display: flex;
        flex-direction: column;
        align-items: flex-start;
        padding: 10px;
        overflow: scroll;
    }
</style>
