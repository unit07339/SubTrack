/**
 * SubTrack API Client Module
 * Supports both live Serverless HTTP API and In-Browser LocalStorage Mock for standalone static execution.
 */
const SubTrackAPI = (() => {
    const BASE_URL = window.location.origin.startsWith('http') ? window.location.origin : '';
    const STORAGE_KEY = 'subtrack_local_subscriptions';

    // Standard Initial Seed Data for offline/standalone preview
    const INITIAL_SEEDS = [
        {
            id: 'sub-seed-1',
            name: '넷플릭스 (Netflix)',
            cost: 17000,
            currency: 'KRW',
            billingCycle: 'MONTHLY',
            billingDay: 25,
            nextBillingDate: '2026-10-25',
            category: 'ENTERTAINMENT',
            paymentMethod: '신한카드',
            status: 'ACTIVE',
            iconKey: 'netflix',
            memo: '프리미엄 4인 파티',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        },
        {
            id: 'sub-seed-2',
            name: 'ChatGPT Plus',
            cost: 29000,
            currency: 'KRW',
            billingCycle: 'MONTHLY',
            billingDay: 18,
            nextBillingDate: '2026-10-18',
            category: 'PRODUCTIVITY',
            paymentMethod: '현대카드',
            status: 'ACTIVE',
            iconKey: 'chatgpt',
            memo: '업무 및 연구용 AI',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        },
        {
            id: 'sub-seed-3',
            name: '유튜브 프리미엄',
            cost: 14900,
            currency: 'KRW',
            billingCycle: 'MONTHLY',
            billingDay: 17,
            nextBillingDate: '2026-10-17',
            category: 'ENTERTAINMENT',
            paymentMethod: '카카오페이',
            status: 'ACTIVE',
            iconKey: 'youtube',
            memo: '광고 제거 및 유튜브 뮤직',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        },
        {
            id: 'sub-seed-4',
            name: '쿠팡 와우 멤버십',
            cost: 7890,
            currency: 'KRW',
            billingCycle: 'MONTHLY',
            billingDay: 5,
            nextBillingDate: '2026-11-05',
            category: 'LIFESTYLE',
            paymentMethod: '쿠페이',
            status: 'ACTIVE',
            iconKey: 'coupang',
            memo: '로켓배송 무료',
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        }
    ];

    function getLocalStore() {
        const raw = localStorage.getItem(STORAGE_KEY);
        if (!raw) {
            localStorage.setItem(STORAGE_KEY, JSON.stringify(INITIAL_SEEDS));
            return INITIAL_SEEDS;
        }
        try {
            return JSON.parse(raw);
        } catch {
            return INITIAL_SEEDS;
        }
    }

    function setLocalStore(data) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    }

    // Calculation helper for local mock
    function calculateNextDate(billingDay, cycle) {
        const today = new Date();
        const year = today.getFullYear();
        const month = today.getMonth();
        const candidate = new Date(year, month, billingDay);
        if (candidate >= today) {
            return candidate.toISOString().split('T')[0];
        }
        return new Date(year, month + 1, billingDay).toISOString().split('T')[0];
    }

    async function request(url, options = {}) {
        // Try live HTTP API first if served via server
        if (window.location.protocol.startsWith('http')) {
            try {
                const res = await fetch(url, {
                    ...options,
                    headers: {
                        'Content-Type': 'application/json',
                        ...(options.headers || {})
                    }
                });
                if (res.ok) {
                    const envelope = await res.json();
                    return envelope.data;
                }
            } catch (err) {
                console.warn('[SubTrack API] Live API fetch failed, falling back to local storage mock.', err);
            }
        }

        // Mock Fallback implementation
        return handleMockRequest(url, options);
    }

    function handleMockRequest(url, options) {
        const store = getLocalStore();
        const path = url.split('?')[0];

        // 1. Health
        if (path.endsWith('/api/health')) {
            return { status: 'UP', service: 'SubTrack Mock Client', version: '1.0.0' };
        }

        // 2. Presets
        if (path.endsWith('/api/presets')) {
            return [
                { key: 'netflix', name: '넷플릭스 (Netflix)', defaultCost: 17000, category: 'ENTERTAINMENT', colorHex: '#E50914' },
                { key: 'youtube', name: '유튜브 프리미엄', defaultCost: 14900, category: 'ENTERTAINMENT', colorHex: '#FF0000' },
                { key: 'disney', name: '디즈니+', defaultCost: 9900, category: 'ENTERTAINMENT', colorHex: '#113CCF' },
                { key: 'spotify', name: '스포티파이', defaultCost: 10900, category: 'MUSIC', colorHex: '#1DB954' },
                { key: 'chatgpt', name: 'ChatGPT Plus', defaultCost: 29000, category: 'PRODUCTIVITY', colorHex: '#10A37F' },
                { key: 'coupang', name: '쿠팡 와우 멤버십', defaultCost: 7890, category: 'LIFESTYLE', colorHex: '#0073E6' },
                { key: 'naver', name: '네이버플러스 멤버십', defaultCost: 4900, category: 'LIFESTYLE', colorHex: '#03C75A' },
                { key: 'github', name: 'GitHub Copilot', defaultCost: 13000, category: 'PRODUCTIVITY', colorHex: '#24292E' }
            ];
        }

        // 3. Analytics Summary
        if (path.endsWith('/api/analytics/summary')) {
            const today = new Date();
            let monthlyTotal = 0;
            let yearlyProjectedTotal = 0;
            let activeCount = 0;
            let pausedCount = 0;
            const categoryBreakdown = {
                ENTERTAINMENT: 0, PRODUCTIVITY: 0, MUSIC: 0, LIFESTYLE: 0, EDUCATION: 0, OTHER: 0
            };
            const upcomingList = [];

            for (const item of store) {
                if (item.status === 'ACTIVE') {
                    activeCount++;
                    const mCost = item.billingCycle === 'YEARLY' ? Math.round(item.cost / 12) : item.cost;
                    const yCost = item.billingCycle === 'YEARLY' ? item.cost : item.cost * 12;
                    monthlyTotal += mCost;
                    yearlyProjectedTotal += yCost;
                    categoryBreakdown[item.category] = (categoryBreakdown[item.category] || 0) + mCost;

                    if (item.nextBillingDate) {
                        const target = new Date(item.nextBillingDate);
                        const diffTime = target - today;
                        const daysLeft = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
                        if (daysLeft >= 0 && daysLeft <= 7) {
                            upcomingList.push({ subscription: item, daysLeft: Math.max(0, daysLeft) });
                        }
                    }
                } else if (item.status === 'PAUSED') {
                    pausedCount++;
                }
            }

            upcomingList.sort((a, b) => a.daysLeft - b.daysLeft);
            const topExpensive = store
                .filter(s => s.status === 'ACTIVE')
                .sort((a, b) => b.cost - a.cost)
                .slice(0, 5);

            return {
                monthlyTotal,
                yearlyProjectedTotal,
                activeCount,
                pausedCount,
                upcomingSubscriptions: upcomingList,
                categoryBreakdown,
                topExpensive
            };
        }

        // 4. Subscriptions List
        if (path.endsWith('/api/subscriptions') && (!options.method || options.method === 'GET')) {
            return store;
        }

        // 5. Create Subscription
        if (path.endsWith('/api/subscriptions') && options.method === 'POST') {
            const payload = JSON.parse(options.body);
            payload.id = 'sub-' + Date.now();
            payload.nextBillingDate = calculateNextDate(Number(payload.billingDay), payload.billingCycle);
            payload.status = payload.status || 'ACTIVE';
            payload.createdAt = new Date().toISOString();
            payload.updatedAt = payload.createdAt;
            store.push(payload);
            setLocalStore(store);
            return payload;
        }

        // 6. Delete
        if (options.method === 'DELETE') {
            const id = path.split('/').pop();
            const filtered = store.filter(s => s.id !== id);
            setLocalStore(filtered);
            return { deleted: true, id };
        }

        // 7. Update status (PATCH)
        if (options.method === 'PATCH') {
            const id = path.split('/')[path.split('/').length - 2];
            const body = JSON.parse(options.body);
            const item = store.find(s => s.id === id);
            if (item) {
                item.status = body.status;
                item.updatedAt = new Date().toISOString();
                setLocalStore(store);
            }
            return item;
        }

        // 8. Update (PUT)
        if (options.method === 'PUT') {
            const id = path.split('/').pop();
            const body = JSON.parse(options.body);
            const idx = store.findIndex(s => s.id === id);
            if (idx !== -1) {
                store[idx] = { ...store[idx], ...body, updatedAt: new Date().toISOString() };
                store[idx].nextBillingDate = calculateNextDate(Number(store[idx].billingDay), store[idx].billingCycle);
                setLocalStore(store);
                return store[idx];
            }
        }

        return null;
    }

    return {
        getHealth: () => request(`${BASE_URL}/api/health`),
        getPresets: () => request(`${BASE_URL}/api/presets`),
        getAnalyticsSummary: () => request(`${BASE_URL}/api/analytics/summary`),
        getSubscriptions: (status, category) => {
            const params = new URLSearchParams();
            if (status) params.append('status', status);
            if (category) params.append('category', category);
            const query = params.toString() ? `?${params.toString()}` : '';
            return request(`${BASE_URL}/api/subscriptions${query}`);
        },
        createSubscription: (data) => request(`${BASE_URL}/api/subscriptions`, {
            method: 'POST',
            body: JSON.stringify(data)
        }),
        updateSubscription: (id, data) => request(`${BASE_URL}/api/subscriptions/${id}`, {
            method: 'PUT',
            body: JSON.stringify(data)
        }),
        deleteSubscription: (id) => request(`${BASE_URL}/api/subscriptions/${id}`, {
            method: 'DELETE'
        }),
        toggleStatus: (id, status) => request(`${BASE_URL}/api/subscriptions/${id}/status`, {
            method: 'PATCH',
            body: JSON.stringify({ status })
        })
    };
})();
