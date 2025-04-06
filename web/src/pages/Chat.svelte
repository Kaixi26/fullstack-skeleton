<script lang="ts">

    import {Button} from "$shadcn/components/ui/button/index.js";
    import {Avatar, AvatarImage, AvatarFallback} from "$shadcn/components/ui/avatar/index.js";
    import {Input} from "$shadcn/components/ui/input/index.js";
    import {Skeleton} from "$shadcn/components/ui/skeleton/index.js";
    import {onMount, tick} from 'svelte';

    type Message = { user: string, message: string };
    type GetMessagesResponse = { messages: Message[] }

    async function getChat(): Promise<GetMessagesResponse> {
        const response = await fetch("/api/chat/");
        const json = await response.json()
        updateViewport = true;
        return json;
    }

    async function postMessage(name: string, message: string): Promise<GetMessagesResponse> {
        const response = await fetch("/api/chat/message", {
            method: "POST",
            body: JSON.stringify({user: name, message}),
            headers: {"Content-Type": "application/json"},
        });
        const json = await response.json()
        updateViewport = true;
        return json;
    }


    let myName = $state("Anonymous");
    let myMessage = $state("");
    let updateViewport = $state(false);
    let viewport: any;
    let chat: Promise<GetMessagesResponse> = $state(getChat());

    $effect.pre(() => {
        if (!updateViewport) return;
        chat;
        const autoscroll = viewport && viewport.offsetHeight + viewport.scrollTop > viewport.scrollHeight - 50;

        if (autoscroll) {
            tick().then(() => {
                viewport.scrollTo(0, viewport.scrollHeight);
            });
        }

        updateViewport = false;
    });


    onMount(() => {
        const interval = setInterval(() => {
            getChat().then(response => {
                chat = Promise.resolve(response);
            })
        }, 10000);

        return () => clearInterval(interval);
    });

    const handleSendMessage = async (e: SubmitEvent) => {
        e.preventDefault();
        postMessage(myName, myMessage).then(response => {
            chat = Promise.resolve(response);
            updateViewport = true;
        })
        myMessage = "";
    }

</script>

<div class="flex flex-col h-full max-h-full">
    <main bind:this={viewport} class="flex-1 overflow-y-auto p-4 space-y-4 overflow-scroll snap-end">
        <div class="flex-grow"></div>
        {#await chat}
            <Skeleton class="h-[20px] w-[100px] rounded-full"/>
        {:then chat}
            {#each chat.messages as message}
                <div class="flex items-end space-x-2">
                    <Avatar>
                        <AvatarFallback>U</AvatarFallback>
                    </Avatar>
                    <div class="p-2 rounded-lg bg-gray-100 dark:bg-gray-800">
                        <p class="text-sm font-semibold">{message.user}</p>
                        <p class="text-sm">{message.message}</p>
                    </div>
                </div>
            {/each}
        {:catch _}
            Failed getting counter ...
        {/await}
    </main>
    <form class="flex items-center space-x-2 p-2 border-t" onsubmit={handleSendMessage}>
        <Input bind:value={myMessage} class="flex-1" placeholder="Type a message"/>
        <Button type="submit" variant="outline" size="sm">
            Send
        </Button>
    </form>
</div>