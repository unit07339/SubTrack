/**
 * SubTrack Single Page Application Controller
 */

let currentTab = 'dashboard';
let currentFilterStatus = 'ALL';
let currentFilterCategory = '';
let currentCalendarDate = new Date();
let miniChartInstance = null;
let largeChartInstance = null;
let cachedPresets = [];

document.addEventListener('DOMContentLoaded', async () => {
    await initPresets();
    await switchTab('dashboard');
});

// Tab Switcher
async function switchTab(tab) {
    currentTab = tab;

    // Toggle Tab Buttons Styling
    document.querySelectorAll('.nav-tab').forEach(btn => btn.classList.remove('active'));
    const activeBtn = document.getElementById(`tab-${tab}`);
    if (activeBtn) activeBtn.classList.add('active');

    // Toggle View Sections
    const views = ['dashboard', 'subscriptions', 'calendar', 'analytics'];
    views.forEach(v => {
        const el = document.getElementById(`view-${v}`);
        if (el) {
            if (v === tab) {
                el.classList.remove('hidden');
            } else {
                el.classList.add('hidden');
            }
        }
    });

    if (window.lucide) lucide.createIcons();

    // Load Tab Data
    switch (tab) {
        case 'dashboard':
            await renderDashboard();
            break;
        case 'subscriptions':
            await renderSubscriptions();
            break;
        case 'calendar':
            await renderCalendar();
            break;
        case 'analytics':
            await renderAnalytics();
            break;
    }
}

// 1. DASHBOARD RENDERER
async function renderDashboard() {
    try {
        const summary = await SubTrackAPI.getAnalyticsSummary();
        if (!summary) return;

        // Metrics
        document.getElementById('metric-monthly-total').textContent = `₩${summary.monthlyTotal.toLocaleString()}`;
        document.getElementById('metric-yearly-total').textContent = `₩${summary.yearlyProjectedTotal.toLocaleString()}`;
        document.getElementById('metric-active-count').textContent = summary.activeCount;
        document.getElementById('metric-paused-count').textContent = summary.pausedCount;

        // Upcoming Bills List (D-7)
        const upcomingList = document.getElementById('upcoming-list');
        const badge = document.getElementById('upcoming-badge');
        badge.textContent = `${summary.upcomingSubscriptions.length}건 대기`;

        if (summary.upcomingSubscriptions.length === 0) {
            upcomingList.innerHTML = `
                <div class="text-center py-10 text-slate-500 text-sm">
                    <i data-lucide="check-circle" class="w-8 h-8 mx-auto mb-2 opacity-40"></i>
                    향후 7일 이내에 예정된 결제 건이 없습니다.
                </div>
            `;
        } else {
            upcomingList.innerHTML = summary.upcomingSubscriptions.map(item => {
                const sub = item.subscription;
                const ddayText = item.daysLeft === 0 ? 'D-Day (오늘)' : `D-${item.daysLeft}`;
                const badgeClass = item.daysLeft <= 1 ? 'badge-dday-imminent' : 'badge-dday-normal';

                return `
                    <div class="flex items-center justify-between p-3.5 rounded-xl bg-slate-800/50 hover:bg-slate-800 border border-slate-700/50 transition-colors">
                        <div class="flex items-center space-x-3">
                            <div class="w-9 h-9 rounded-lg bg-indigo-950/80 border border-indigo-700/40 flex items-center justify-center font-bold text-indigo-400 text-xs">
                                ${sub.name.charAt(0)}
                            </div>
                            <div>
                                <div class="text-sm font-semibold text-white">${escapeHtml(sub.name)}</div>
                                <div class="text-xs text-slate-400">${sub.nextBillingDate} 결제 예정</div>
                            </div>
                        </div>
                        <div class="text-right">
                            <div class="text-sm font-bold text-white">₩${sub.cost.toLocaleString()}</div>
                            <span class="inline-block mt-0.5 text-[11px] font-bold px-2 py-0.5 rounded-full ${badgeClass}">${ddayText}</span>
                        </div>
                    </div>
                `;
            }).join('');
        }

        // Mini Category Chart
        renderMiniChart(summary.categoryBreakdown);
        if (window.lucide) lucide.createIcons();
    } catch (e) {
        console.error('Failed to render dashboard:', e);
    }
}

function renderMiniChart(breakdown) {
    const canvas = document.getElementById('chart-category-mini');
    if (!canvas) return;

    const labels = Object.keys(breakdown);
    const data = Object.values(breakdown);
    const hasData = data.some(v => v > 0);

    if (miniChartInstance) {
        miniChartInstance.destroy();
    }

    const categoryNames = {
        ENTERTAINMENT: '엔터테인먼트',
        PRODUCTIVITY: '생산성/업무',
        MUSIC: '음원/오디오',
        LIFESTYLE: '생활/쇼핑',
        EDUCATION: '교육/도서',
        OTHER: '기타'
    };

    miniChartInstance = new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: labels.map(l => categoryNames[l] || l),
            datasets: [{
                data: hasData ? data : [1],
                backgroundColor: hasData ? [
                    '#6366f1', '#ec4899', '#10b981', '#f59e0b', '#3b82f6', '#8b5cf6'
                ] : ['#334155'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: { color: '#94a3b8', font: { size: 11 } }
                }
            },
            cutout: '70%'
        }
    });
}

// 2. SUBSCRIPTIONS LIST RENDERER
async function renderSubscriptions() {
    const grid = document.getElementById('subscription-grid');
    if (!grid) return;

    try {
        const subs = await SubTrackAPI.getSubscriptions(
            currentFilterStatus === 'ALL' ? '' : currentFilterStatus,
            currentFilterCategory
        );

        if (!subs || subs.length === 0) {
            grid.innerHTML = `
                <div class="col-span-full text-center py-16 text-slate-500">
                    <i data-lucide="inbox" class="w-12 h-12 mx-auto mb-3 opacity-30"></i>
                    <p class="text-base font-semibold">등록된 구독 서비스가 없습니다.</p>
                    <p class="text-xs text-slate-500 mt-1">상단의 '구독 추가' 버튼을 눌러 첫 구독을 등록해 보세요.</p>
                </div>
            `;
            if (window.lucide) lucide.createIcons();
            return;
        }

        grid.innerHTML = subs.map(sub => {
            const isPaused = sub.status === 'PAUSED';
            const statusBadge = isPaused
                ? '<span class="text-[11px] font-bold px-2 py-0.5 rounded-full bg-slate-700 text-slate-300">일시정지</span>'
                : '<span class="text-[11px] font-bold px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">활성</span>';

            const cycleText = sub.billingCycle === 'YEARLY' ? '연간' : (sub.billingCycle === 'WEEKLY' ? '주간' : '매월');

            return `
                <div class="glass-card p-5 rounded-2xl flex flex-col justify-between space-y-4">
                    <div class="flex items-start justify-between">
                        <div class="flex items-center space-x-3">
                            <div class="w-10 h-10 rounded-xl bg-brand-950/60 border border-brand-800/40 flex items-center justify-center font-bold text-brand-400">
                                ${sub.name.charAt(0)}
                            </div>
                            <div>
                                <h3 class="text-base font-bold text-white">${escapeHtml(sub.name)}</h3>
                                <div class="text-xs text-slate-400">${sub.category}</div>
                            </div>
                        </div>
                        ${statusBadge}
                    </div>

                    <div class="bg-slate-900/60 p-3 rounded-xl border border-slate-800 space-y-1.5 text-xs">
                        <div class="flex justify-between">
                            <span class="text-slate-400">결제 금액:</span>
                            <span class="font-bold text-white">₩${sub.cost.toLocaleString()} <span class="text-[10px] text-slate-400">(${cycleText})</span></span>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-slate-400">다음 결제일:</span>
                            <span class="text-indigo-400 font-semibold">${sub.nextBillingDate} (매월 ${sub.billingDay}일)</span>
                        </div>
                        ${sub.paymentMethod ? `
                        <div class="flex justify-between">
                            <span class="text-slate-400">결제 수단:</span>
                            <span class="text-slate-300">${escapeHtml(sub.paymentMethod)}</span>
                        </div>` : ''}
                    </div>

                    ${sub.memo ? `<div class="text-xs text-slate-400 italic">"${escapeHtml(sub.memo)}"</div>` : ''}

                    <div class="flex items-center justify-between pt-2 border-t border-slate-800">
                        <button onclick="toggleSubscriptionStatus('${sub.id}', '${sub.status}')" class="text-xs text-slate-400 hover:text-white flex items-center space-x-1">
                            <i data-lucide="${isPaused ? 'play' : 'pause'}" class="w-3.5 h-3.5"></i>
                            <span>${isPaused ? '재개하기' : '일시정지'}</span>
                        </button>
                        <button onclick="deleteSubscription('${sub.id}')" class="text-xs text-rose-400 hover:text-rose-300 flex items-center space-x-1">
                            <i data-lucide="trash-2" class="w-3.5 h-3.5"></i>
                            <span>삭제</span>
                        </button>
                    </div>
                </div>
            `;
        }).join('');

        if (window.lucide) lucide.createIcons();
    } catch (e) {
        console.error('Failed to render subscriptions:', e);
    }
}

// 3. CALENDAR RENDERER
async function renderCalendar() {
    const daysContainer = document.getElementById('calendar-days');
    const title = document.getElementById('calendar-title');
    if (!daysContainer) return;

    const year = currentCalendarDate.getFullYear();
    const month = currentCalendarDate.getMonth();
    title.textContent = `${year}년 ${month + 1}월 결제 캘린더`;

    const firstDayIndex = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();

    const subs = await SubTrackAPI.getSubscriptions();
    const billsByDay = {};
    if (subs) {
        subs.forEach(s => {
            if (s.status === 'ACTIVE') {
                const day = s.billingDay;
                if (!billsByDay[day]) billsByDay[day] = [];
                billsByDay[day].push(s);
            }
        });
    }

    let html = '';
    for (let i = 0; i < firstDayIndex; i++) {
        html += `<div class="h-20 bg-slate-900/30 rounded-xl border border-slate-800/30"></div>`;
    }

    const today = new Date();
    for (let d = 1; d <= daysInMonth; d++) {
        const isToday = today.getFullYear() === year && today.getMonth() === month && today.getDate() === d;
        const bills = billsByDay[d] || [];

        html += `
            <div class="h-24 p-1.5 bg-slate-800/40 rounded-xl border ${isToday ? 'border-brand-500 bg-brand-950/20' : 'border-slate-800'} flex flex-col justify-between hover:border-slate-600 transition-colors">
                <div class="flex justify-between items-center text-xs font-semibold ${isToday ? 'text-brand-400 font-bold' : 'text-slate-400'}">
                    <span>${d}</span>
                    ${bills.length > 0 ? `<span class="text-[10px] text-indigo-400">${bills.length}건</span>` : ''}
                </div>
                <div class="space-y-1 overflow-y-auto max-h-14">
                    ${bills.map(b => `
                        <div class="text-[10px] truncate px-1.5 py-0.5 rounded bg-indigo-950/80 border border-indigo-700/50 text-indigo-200">
                            ${escapeHtml(b.name)}
                        </div>
                    `).join('')}
                </div>
            </div>
        `;
    }

    daysContainer.innerHTML = html;
}

function changeMonth(delta) {
    currentCalendarDate.setMonth(currentCalendarDate.getMonth() + delta);
    renderCalendar();
}

function resetMonth() {
    currentCalendarDate = new Date();
    renderCalendar();
}

// 4. ANALYTICS RENDERER
async function renderAnalytics() {
    try {
        const summary = await SubTrackAPI.getAnalyticsSummary();
        if (!summary) return;

        // Render Large Doughnut Chart
        const canvas = document.getElementById('chart-category-large');
        if (canvas) {
            if (largeChartInstance) largeChartInstance.destroy();

            const labels = Object.keys(summary.categoryBreakdown);
            const data = Object.values(summary.categoryBreakdown);

            largeChartInstance = new Chart(canvas, {
                type: 'pie',
                data: {
                    labels: labels,
                    datasets: [{
                        data: data,
                        backgroundColor: ['#6366f1', '#ec4899', '#10b981', '#f59e0b', '#3b82f6', '#8b5cf6'],
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { position: 'right', labels: { color: '#94a3b8' } }
                    }
                }
            });
        }

        // Render Top 5
        const topList = document.getElementById('top-expensive-list');
        if (topList) {
            topList.innerHTML = summary.topExpensive.map((sub, idx) => `
                <div class="flex items-center justify-between p-3.5 bg-slate-800/40 border border-slate-700/50 rounded-xl">
                    <div class="flex items-center space-x-3">
                        <span class="w-6 h-6 rounded-full bg-slate-700 text-xs font-bold flex items-center justify-center text-slate-300">${idx + 1}</span>
                        <div>
                            <div class="text-sm font-bold text-white">${escapeHtml(sub.name)}</div>
                            <div class="text-xs text-slate-400">${sub.category}</div>
                        </div>
                    </div>
                    <div class="text-sm font-bold text-white">₩${sub.cost.toLocaleString()}</div>
                </div>
            `).join('');
        }
    } catch (e) {
        console.error('Failed to render analytics:', e);
    }
}

// Modal & Form Handlers
async function initPresets() {
    try {
        cachedPresets = await SubTrackAPI.getPresets();
        const container = document.getElementById('preset-buttons');
        if (container && cachedPresets) {
            container.innerHTML = cachedPresets.map(p => `
                <button type="button" onclick="selectPreset('${p.key}')" class="px-2.5 py-1 text-xs rounded-lg bg-slate-800 border border-slate-700 text-slate-300 hover:text-white hover:border-brand-500 transition-colors">
                    ${escapeHtml(p.name)}
                </button>
            `).join('');
        }
    } catch (e) {
        console.error('Failed to init presets:', e);
    }
}

function selectPreset(key) {
    const preset = cachedPresets.find(p => p.key === key);
    if (!preset) return;

    document.getElementById('sub-name').value = preset.name;
    document.getElementById('sub-cost').value = preset.defaultCost;
    document.getElementById('sub-category').value = preset.category;
    document.getElementById('sub-billing-day').value = 25;
}

function openSubscriptionModal() {
    document.getElementById('form-subscription').reset();
    document.getElementById('sub-id').value = '';
    document.getElementById('modal-title').textContent = '새 구독 서비스 등록';
    document.getElementById('modal-subscription').classList.remove('hidden');
}

function closeSubscriptionModal() {
    document.getElementById('modal-subscription').classList.add('hidden');
}

async function handleSubscriptionSubmit(e) {
    e.preventDefault();

    const payload = {
        name: document.getElementById('sub-name').value,
        cost: Number(document.getElementById('sub-cost').value),
        billingDay: Number(document.getElementById('sub-billing-day').value),
        billingCycle: document.getElementById('sub-cycle').value,
        category: document.getElementById('sub-category').value,
        paymentMethod: document.getElementById('sub-payment-method').value,
        memo: document.getElementById('sub-memo').value
    };

    try {
        await SubTrackAPI.createSubscription(payload);
        closeSubscriptionModal();
        await switchTab(currentTab);
    } catch (err) {
        alert('구독 등록 실패: ' + err.message);
    }
}

async function deleteSubscription(id) {
    if (!confirm('정말 이 구독 서비스를 삭제하시겠습니까?')) return;
    try {
        await SubTrackAPI.deleteSubscription(id);
        await switchTab(currentTab);
    } catch (err) {
        alert('삭제 실패: ' + err.message);
    }
}

async function toggleSubscriptionStatus(id, currentStatus) {
    const newStatus = currentStatus === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    try {
        await SubTrackAPI.toggleStatus(id, newStatus);
        await switchTab(currentTab);
    } catch (err) {
        alert('상태 변경 실패: ' + err.message);
    }
}

function filterSubscriptions(status) {
    currentFilterStatus = status;
    document.querySelectorAll('.sub-filter-btn').forEach(btn => btn.classList.remove('active', 'bg-indigo-600', 'text-white'));
    event.target.classList.add('active', 'bg-indigo-600', 'text-white');
    renderSubscriptions();
}

function filterByCategory(cat) {
    currentFilterCategory = cat;
    renderSubscriptions();
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
