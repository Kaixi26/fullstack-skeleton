<script lang="ts">

    import { Button } from "$shadcn/components/ui/button/index.js";

    type GetCounterResponse = { count: number };

    async function getCounter(): Promise<GetCounterResponse> {
        const response = await fetch("/api/counter");
        return response.json();
    }

    async function postCounter(delta: number): Promise<GetCounterResponse> {
        const response = await fetch("/api/counter", {
            method: "POST",
            body: JSON.stringify({delta: Math.floor(delta)}),
            headers: {"Content-Type": "application/json"},
        });
        return response.json();
    }

    let counter: Promise<GetCounterResponse> = $state(getCounter())

    function updateCounter(delta: number) {
        return () => {
            postCounter(delta).then(response => {
                counter = Promise.resolve(response);
            })
        }
    }

</script>

{#await counter}
    Getting counter ...
{:then counter}
    <Button onclick={updateCounter((-1))}>
        -
    </Button>
    <button>
        count is {counter.count}
    </button>
    <Button onclick={updateCounter(1)}>
        +
    </Button>
{:catch _}
    Failed getting counter ...
{/await}
