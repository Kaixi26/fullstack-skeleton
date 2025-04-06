<script lang="ts">
    // @ts-ignore
    import * as Sidebar from "$shadcn/components/ui/sidebar/index.js";
    import AppSidebar from "./components/app-sidebar.svelte";
    import * as Lucide from "@lucide/svelte";
    import {Router, Route} from "svelte-routing";

    import PageChat from "./pages/Chat.svelte";
    import PageCounter from "./pages/Counter.svelte";

    let {url} = $props();

</script>

<Router {url}>

    <!--
    <h1 class="text-3xl font-bold underline">
        Hello world!
    </h1>

    <div class="card">
        <Counter/>
    </div>

    <div class="card">
        <Chat/>
    </div>
    -->

    <Sidebar.Provider>
        <AppSidebar/>
        <Sidebar.Inset>
            <header class="flex h-16 shrink-0 items-center gap-2 border-b px-4">
                <Sidebar.Trigger class="-ml-1"/>
            </header>
            <main class="flex flex-1 flex-col gap-4 p-4">
                <Route path="/">
                    <PageChat/>
                </Route>

                <Route path="/counter">
                    <PageCounter/>
                </Route>

                <Route path="/*">Page not found.</Route>
            </main>
        </Sidebar.Inset>
    </Sidebar.Provider>
</Router>

