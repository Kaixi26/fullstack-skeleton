<script lang="ts">
    import * as Sidebar from "$shadcn/components/ui/sidebar/index.js";
    import * as Lucide from "@lucide/svelte/icons";
    import {Link} from "svelte-routing";

    const items = [
        {title: "Chat", url: "/", icon: Lucide.House},
        {title: "Counter", url: "/counter", icon: Lucide.Inbox},
    ];
</script>

<Sidebar.Root>
    <Sidebar.Content>
        <Sidebar.Group>
            <Sidebar.GroupLabel>Application</Sidebar.GroupLabel>
            <Sidebar.GroupContent>
                <Sidebar.Menu>
                    {#each items as item (item.title)}
                        <Sidebar.MenuItem>
                            <Sidebar.MenuButton>
                                {#snippet child({props})}
                                    <Link to={item.url} {...props}>
                                        <item.icon/>
                                        <span>{item.title}</span>
                                    </Link>
                                {/snippet}
                            </Sidebar.MenuButton>
                        </Sidebar.MenuItem>
                    {/each}
                </Sidebar.Menu>
            </Sidebar.GroupContent>
        </Sidebar.Group>
    </Sidebar.Content>
</Sidebar.Root>

