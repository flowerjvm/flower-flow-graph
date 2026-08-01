const SVG_NS = "http://www.w3.org/2000/svg";
const NODE_WIDTH = 178;
const NODE_HEIGHT = 86;
const MIN_ZOOM = 0.4;
const MAX_ZOOM = 2.5;
const BUTTON_ZOOM_FACTOR = 1.2;
const WORKSPACE_SCHEMA_VERSION = "flower.flow-graph-workspace/v1";

const MESSAGES = {
    en: {
        "project.loadingLabel": "LOADING PROJECT",
        "project.loading": "Loading project…",
        "project.label": "PROJECT",
        "project.failedLabel": "ANALYSIS FAILED",
        "project.failed": "Could not load project",
        "project.noRevision": "No revision",
        "project.savedLabel": "SAVED WORKSPACE",
        "project.workspaceErrorLabel": "WORKSPACE ERROR",
        "action.reanalyze": "Re-analyze",
        "action.loadProject": "Load project",
        "action.analyzing": "Analyzing…",
        "action.openWorkspace": "Open file",
        "action.saveWorkspace": "Save file",
        "sidebar.workers": "Worker definitions",
        "sidebar.definitions": "Flow definitions",
        "sidebar.flowMap": "Project Flow Map",
        "sidebar.flowMapDescription": "Submission relationships between Flows",
        "sidebar.searchLabel": "Search Flows",
        "sidebar.searchPlaceholder": "Search Flow or file",
        "graph.selectFlow": "Select a Flow",
        "graph.zoomControls": "Graph zoom controls",
        "graph.zoomOutTitle": "Zoom out (Ctrl + wheel down)",
        "graph.zoomOut": "Zoom graph out",
        "graph.zoomReset": "Reset to 100%",
        "graph.zoomInTitle": "Zoom in (Ctrl + wheel up)",
        "graph.zoomIn": "Zoom graph in",
        "graph.legend": "Graph legend",
        "graph.emptyTitle": "No Flow to display",
        "graph.emptyDescription": "Select a Flow definition on the left.",
        "graph.ariaLabel": "Flower Flow structure graph",
        "graph.analysisFailed": "Could not analyze the source",
        "graph.projectFlowMap": "Project Flow Map",
        "graph.flowMapDescription": "These are separate Flow submissions found in Java source, not Flow lifecycle containment.",
        "graph.workerFlowMap": "Worker Flow Map",
        "graph.workerFlowMapDescription": "Flows submitted to this Worker found in source.",
        "graph.connectedFlows": "{count} connected Flows",
        "graph.partialAnalysis": "Partial analysis",
        "graph.staticAnalysis": "Static analysis",
        "graph.otherDefinitions": "OTHER FLOW DEFINITIONS · {count} NOT CONNECTED",
        "graph.openDefinition": "{count} Steps · Click to open",
        "graph.targetUnknown": "Target definition not found statically",
        "legend.source": "Source",
        "legend.linkedFlow": "Linked Flow",
        "inspector.details": "Details",
        "inspector.selectStep": "Select a Step to see information found in the source.",
        "definition.partial": "Dynamic or partial analysis",
        "definition.noResults": "No search results.",
        "relation.conditional": "submits · conditional",
        "relation.callSites": "submits · {count} call sites",
        "detail.partial": "Partial analysis",
        "detail.sourceConfirmed": "Confirmed in source",
        "detail.relation": "Relationship",
        "detail.separateSubmission": "Separate Flow submission (submits)",
        "detail.submissionStep": "Submitting Step",
        "detail.certainty": "Certainty",
        "detail.callSite": "Call site",
        "detail.workerName": "Worker name",
        "detail.workerKind": "Worker kind",
        "detail.definitionSource": "Definition source",
        "detail.connectedFlows": "Connected Flows",
        "detail.submissionSites": "Submission sites",
        "detail.unknown": "Unknown",
        "detail.stepImplementation": "Step implementation",
        "detail.declaration": "Declaration",
        "detail.present": "Present",
        "detail.absent": "None",
        "detail.durability": "Durability",
        "detail.regularStep": "Regular Step",
        "detail.confirmedResult": "Confirmed StepResult",
        "detail.notConfirmed": "Not confirmed statically",
        "detail.eventSubscriptions": "Source event subscriptions",
        "detail.subscription": "subscribe",
        "detail.awaitEvent": "await event",
        "detail.filtered": "filtered",
        "detail.emitsSignal": "emits signal",
        "detail.internalPhases": "Internal stepNo phases",
        "detail.internalPartial": "Some internal cases or targets are computed and are not shown as facts.",
        "detail.timeoutStart": "starts timeout",
        "detail.timeoutCheck": "checks timeout",
        "detail.signals": "signals",
        "detail.events": "events",
        "detail.transitionSetStepNo": "setStepNo",
        "detail.transitionTimeout": "timeout",
        "notice.summary": "Analysis notices: {count}",
        "workspace.invalid": "This is not a valid Flower Flow Graph workspace file.",
        "workspace.readFailed": "Could not open the workspace file.",
        "workspace.savedAt": "{fileName} · saved {savedAt}"
    },
    ko: {
        "project.loadingLabel": "프로젝트 불러오는 중",
        "project.loading": "프로젝트를 불러오는 중…",
        "project.label": "프로젝트",
        "project.failedLabel": "분석 실패",
        "project.failed": "프로젝트를 불러오지 못했습니다",
        "project.noRevision": "리비전 없음",
        "project.savedLabel": "저장한 작업 파일",
        "project.workspaceErrorLabel": "작업 파일 오류",
        "action.reanalyze": "다시 분석",
        "action.loadProject": "프로젝트 불러오기",
        "action.analyzing": "분석 중…",
        "action.openWorkspace": "파일 열기",
        "action.saveWorkspace": "파일 저장",
        "sidebar.workers": "Worker 정의",
        "sidebar.definitions": "Flow 정의",
        "sidebar.flowMap": "프로젝트 Flow Map",
        "sidebar.flowMapDescription": "Flow 사이의 제출 관계",
        "sidebar.searchLabel": "Flow 검색",
        "sidebar.searchPlaceholder": "Flow 또는 파일 검색",
        "graph.selectFlow": "Flow를 선택하세요",
        "graph.zoomControls": "그래프 확대 및 축소",
        "graph.zoomOutTitle": "축소 (Ctrl + 휠 아래)",
        "graph.zoomOut": "그래프 축소",
        "graph.zoomReset": "100%로 초기화",
        "graph.zoomInTitle": "확대 (Ctrl + 휠 위)",
        "graph.zoomIn": "그래프 확대",
        "graph.legend": "그래프 범례",
        "graph.emptyTitle": "표시할 Flow가 없습니다",
        "graph.emptyDescription": "왼쪽에서 Flow 정의를 선택하세요.",
        "graph.ariaLabel": "Flower Flow 구조 그래프",
        "graph.analysisFailed": "소스를 분석하지 못했습니다",
        "graph.projectFlowMap": "프로젝트 Flow Map",
        "graph.flowMapDescription": "Flow 수명 포함 관계가 아니라, Java 소스에서 확인한 별도 Flow 제출 관계입니다.",
        "graph.workerFlowMap": "Worker Flow 관계",
        "graph.workerFlowMapDescription": "소스에서 이 Worker로 제출되는 것으로 확인한 Flow입니다.",
        "graph.connectedFlows": "연결 Flow {count}개",
        "graph.partialAnalysis": "부분 분석",
        "graph.staticAnalysis": "정적 분석",
        "graph.otherDefinitions": "OTHER FLOW DEFINITIONS · 연결 관계 없음 {count}",
        "graph.openDefinition": "{count} Steps · 클릭해서 열기",
        "graph.targetUnknown": "대상 정의를 정적으로 확인하지 못함",
        "legend.source": "소스",
        "legend.linkedFlow": "연결 Flow",
        "inspector.details": "정보",
        "inspector.selectStep": "그래프에서 Step을 선택하면 소스에서 확인한 정보를 보여줍니다.",
        "definition.partial": "동적 또는 부분 분석",
        "definition.noResults": "검색 결과가 없습니다.",
        "relation.conditional": "submits · 조건부",
        "relation.callSites": "submits · 호출 위치 {count}곳",
        "detail.partial": "부분 분석",
        "detail.sourceConfirmed": "소스에서 확인",
        "detail.relation": "관계",
        "detail.separateSubmission": "별도 Flow 제출(submits)",
        "detail.submissionStep": "제출 Step",
        "detail.certainty": "확실성",
        "detail.callSite": "호출 위치",
        "detail.workerName": "Worker 이름",
        "detail.workerKind": "Worker 종류",
        "detail.definitionSource": "정의 방식",
        "detail.connectedFlows": "연결 Flow",
        "detail.submissionSites": "제출 위치",
        "detail.unknown": "확인되지 않음",
        "detail.stepImplementation": "Step 구현",
        "detail.declaration": "선언 위치",
        "detail.present": "있음",
        "detail.absent": "없음",
        "detail.durability": "내구성",
        "detail.regularStep": "일반 Step",
        "detail.confirmedResult": "확인된 StepResult",
        "detail.notConfirmed": "정적으로 확인되지 않음",
        "detail.eventSubscriptions": "소스 이벤트 구독",
        "detail.subscription": "구독",
        "detail.awaitEvent": "이벤트 대기",
        "detail.filtered": "조건 확인",
        "detail.emitsSignal": "signal 전달",
        "detail.internalPhases": "Step 내부 stepNo 단계",
        "detail.internalPartial": "계산되는 내부 case 또는 대상은 사실로 단정하지 않고 표시하지 않았습니다.",
        "detail.timeoutStart": "timeout 시작",
        "detail.timeoutCheck": "timeout 확인",
        "detail.signals": "signals",
        "detail.events": "events",
        "detail.transitionSetStepNo": "setStepNo",
        "detail.transitionTimeout": "timeout",
        "notice.summary": "분석 주의사항 {count}개",
        "workspace.invalid": "Flower Flow Graph 작업 파일 형식이 아닙니다.",
        "workspace.readFailed": "작업 파일을 열지 못했습니다.",
        "workspace.savedAt": "{fileName} · {savedAt} 저장"
    }
};

const requestedLanguage = new URLSearchParams(window.location.search).get("lang")?.toLowerCase();
const preferredLanguage = requestedLanguage === "ko" || requestedLanguage === "en"
    ? requestedLanguage
    : navigator.languages?.[0] || navigator.language || "en";
let locale = preferredLanguage.toLowerCase().startsWith("ko") ? "ko" : "en";
document.documentElement.lang = locale;

const state = {
    document: null,
    documentOrigin: "project",
    loadedFileName: null,
    loadedSavedAt: null,
    viewMode: "definition",
    selectedWorkerId: null,
    selectedDefinitionId: null,
    selectedNodeId: null,
    positionsByGraph: new Map(),
    zoomByKey: new Map(),
    layoutSignatureByKey: new Map(),
    collapsedSidebarSections: new Set(),
    drag: null,
    pan: null
};

const elements = {
    projectStatus: document.querySelector("#project-status"),
    projectStatusLabel: document.querySelector("#project-status-label"),
    projectName: document.querySelector("#project-name"),
    projectRevision: document.querySelector("#project-revision"),
    openWorkspace: document.querySelector("#open-workspace"),
    workspaceFileInput: document.querySelector("#workspace-file-input"),
    saveWorkspace: document.querySelector("#save-workspace"),
    workerSection: document.querySelector(".worker-section"),
    workerSectionToggle: document.querySelector("#worker-section-toggle"),
    workerSectionContent: document.querySelector("#worker-section-content"),
    workerCount: document.querySelector("#worker-count"),
    workerList: document.querySelector("#worker-list"),
    flowSection: document.querySelector(".flow-section"),
    flowSectionToggle: document.querySelector("#flow-section-toggle"),
    flowSectionContent: document.querySelector("#flow-section-content"),
    definitionCount: document.querySelector("#definition-count"),
    relationCount: document.querySelector("#relation-count"),
    definitionSearch: document.querySelector("#definition-search"),
    definitionList: document.querySelector("#definition-list"),
    flowMapButton: document.querySelector("#flow-map-button"),
    refreshButton: document.querySelector("#refresh-button"),
    flowTitle: document.querySelector("#flow-title"),
    flowKind: document.querySelector("#flow-kind"),
    flowCompleteness: document.querySelector("#flow-completeness"),
    flowSource: document.querySelector("#flow-source"),
    graphEmpty: document.querySelector("#graph-empty"),
    graphScroll: document.querySelector("#graph-scroll"),
    graph: document.querySelector("#graph"),
    zoomOut: document.querySelector("#zoom-out"),
    zoomReset: document.querySelector("#zoom-reset"),
    zoomIn: document.querySelector("#zoom-in"),
    noticeStrip: document.querySelector("#notice-strip"),
    noticeToggle: document.querySelector("#notice-toggle"),
    noticeSummary: document.querySelector("#notice-summary"),
    noticeList: document.querySelector("#notice-list"),
    nodeEmpty: document.querySelector("#node-empty"),
    nodeDetails: document.querySelector("#node-details")
};

document.addEventListener("DOMContentLoaded", () => {
    applyTranslations();
    bindEvents();
    loadGraph(false);
});

function bindEvents() {
    elements.refreshButton.addEventListener("click", () => loadGraph(true));
    elements.openWorkspace.addEventListener("click", () => elements.workspaceFileInput.click());
    elements.workspaceFileInput.addEventListener("change", openWorkspaceFile);
    elements.saveWorkspace.addEventListener("click", saveWorkspaceFile);
    elements.workerSectionToggle.addEventListener("click", () =>
        toggleSidebarSection("workers"));
    elements.flowSectionToggle.addEventListener("click", () =>
        toggleSidebarSection("flows"));
    elements.definitionSearch.addEventListener("input", renderDefinitionList);
    elements.flowMapButton.addEventListener("click", () => {
        state.viewMode = "map";
        state.selectedNodeId = null;
        renderAll();
    });
    elements.noticeToggle.addEventListener("click", () => {
        elements.noticeList.hidden = !elements.noticeList.hidden;
    });

    elements.zoomOut.addEventListener("click", () => zoomFromCenter(1 / BUTTON_ZOOM_FACTOR));
    elements.zoomReset.addEventListener("click", () => setZoomAt(1));
    elements.zoomIn.addEventListener("click", () => zoomFromCenter(BUTTON_ZOOM_FACTOR));
    elements.graphScroll.addEventListener("wheel", event => {
        if (!event.ctrlKey && !event.metaKey) return;
        event.preventDefault();
        const currentZoom = zoomForSelected();
        const factor = Math.exp(-event.deltaY * 0.0015);
        setZoomAt(currentZoom * factor, event.clientX, event.clientY);
    }, {passive: false});
    elements.graphScroll.addEventListener("pointerdown", event => {
        if (event.button !== 0) return;
        const target = event.target instanceof Element ? event.target : null;
        if (target?.closest(".graph-node")) return;
        state.pan = {
            pointerId: event.pointerId,
            startX: event.clientX,
            startY: event.clientY,
            scrollLeft: elements.graphScroll.scrollLeft,
            scrollTop: elements.graphScroll.scrollTop
        };
        elements.graphScroll.classList.add("panning");
        elements.graphScroll.setPointerCapture?.(event.pointerId);
        event.preventDefault();
    });

    window.addEventListener("pointermove", event => {
        if (state.pan && state.pan.pointerId === event.pointerId) {
            elements.graphScroll.scrollLeft =
                state.pan.scrollLeft - (event.clientX - state.pan.startX);
            elements.graphScroll.scrollTop =
                state.pan.scrollTop - (event.clientY - state.pan.startY);
            event.preventDefault();
            return;
        }
        if (!state.drag) return;
        const svgPoint = clientToSvg(event.clientX, event.clientY);
        const positions = positionsForSelected();
        positions.set(state.drag.nodeId, {
            x: Math.max(30, svgPoint.x - state.drag.offsetX),
            y: Math.max(30, svgPoint.y - state.drag.offsetY)
        });
        renderGraph();
    });
    window.addEventListener("pointerup", endPointerInteraction);
    window.addEventListener("pointercancel", endPointerInteraction);
    updateZoomControls();
}

function endPointerInteraction(event) {
    if (state.pan && state.pan.pointerId === event.pointerId) {
        elements.graphScroll.releasePointerCapture?.(event.pointerId);
        elements.graphScroll.classList.remove("panning");
        state.pan = null;
    }
    if (state.drag) {
        state.drag = null;
    }
}

async function loadGraph(isRefresh) {
    setProjectStatus(
        "loading",
        "project.loadingLabel",
        t(isRefresh ? "action.analyzing" : "project.loading"));
    elements.refreshButton.disabled = true;
    elements.refreshButton.textContent = t("action.analyzing");
    try {
        const response = await fetch("/api/graph", {cache: "no-store"});
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || `HTTP ${response.status}`);
        }
        const nextDocument = await response.json();
        state.document = nextDocument;
        state.documentOrigin = "project";
        state.loadedFileName = null;
        state.loadedSavedAt = null;

        const stillExists = nextDocument.definitions.some(
            definition => definition.id === state.selectedDefinitionId);
        if (!stillExists) {
            state.selectedDefinitionId = nextDocument.definitions[0]?.id || null;
            state.selectedNodeId = null;
        }
        const workerStillExists = (nextDocument.workers || []).some(
            worker => worker.id === state.selectedWorkerId);
        if (!workerStillExists) {
            state.selectedWorkerId = null;
            if (state.viewMode === "worker") state.viewMode = "definition";
        }
        renderAll();
    } catch (error) {
        setProjectStatus("error", "project.failedLabel", t("project.failed"));
        if (!state.document) {
            elements.graphEmpty.hidden = false;
            elements.graphScroll.hidden = true;
            elements.graphEmpty.querySelector("h3").textContent = t("graph.analysisFailed");
            elements.graphEmpty.querySelector("p").textContent = error.message;
        }
    } finally {
        elements.refreshButton.disabled = false;
        elements.refreshButton.textContent = t(
            state.documentOrigin === "file" ? "action.loadProject" : "action.reanalyze");
    }
}

function renderAll() {
    const revision = state.document.project.revision
        ? state.document.project.revision.slice(0, 12)
        : t("project.noRevision");
    if (state.documentOrigin === "file") {
        const savedAt = formatSavedAt(state.loadedSavedAt);
        const fileDetail = t("workspace.savedAt", {
            fileName: state.loadedFileName || "workspace",
            savedAt
        });
        setProjectStatus("saved", "project.savedLabel", state.document.project.name, fileDetail);
        elements.projectRevision.title = state.document.project.revision || "";
    } else {
        setProjectStatus("ready", "project.label", state.document.project.name, revision);
        elements.projectRevision.title = state.document.project.revision || "";
    }
    elements.saveWorkspace.disabled = false;
    elements.refreshButton.textContent = t(
        state.documentOrigin === "file" ? "action.loadProject" : "action.reanalyze");
    elements.workerCount.textContent = state.document.workers?.length || 0;
    elements.definitionCount.textContent = state.document.definitions.length;
    elements.relationCount.textContent = state.document.relations?.length || 0;
    elements.flowMapButton.classList.toggle("active", state.viewMode === "map");
    renderSidebarSections();
    renderWorkerList();
    renderDefinitionList();
    renderSelectedDefinition();
    updateZoomControls();
}

function toggleSidebarSection(section) {
    if (state.collapsedSidebarSections.has(section)) {
        state.collapsedSidebarSections.delete(section);
    } else {
        state.collapsedSidebarSections.add(section);
    }
    renderSidebarSections();
}

function renderSidebarSections() {
    const sections = [
        {
            id: "workers",
            section: elements.workerSection,
            toggle: elements.workerSectionToggle,
            content: elements.workerSectionContent
        },
        {
            id: "flows",
            section: elements.flowSection,
            toggle: elements.flowSectionToggle,
            content: elements.flowSectionContent
        }
    ];
    for (const item of sections) {
        const collapsed = state.collapsedSidebarSections.has(item.id);
        item.section.classList.toggle("collapsed", collapsed);
        item.toggle.setAttribute("aria-expanded", String(!collapsed));
        item.content.hidden = collapsed;
    }
}

function renderWorkerList() {
    clear(elements.workerList);
    if (!state.document) return;
    for (const worker of state.document.workers || []) {
        const button = el("button", "definition-item worker-item");
        button.type = "button";
        button.setAttribute("role", "listitem");
        if (state.viewMode === "worker" && worker.id === state.selectedWorkerId) {
            button.classList.add("active");
        }

        const name = el("span", "definition-name");
        const label = el("span");
        label.textContent = worker.name || worker.nameExpression;
        name.append(label);
        if (worker.dynamic) {
            const dot = el("i", "partial-dot");
            dot.title = t("definition.partial");
            name.append(dot);
        }
        const source = el("span", "definition-meta");
        source.textContent = `${worker.source.file}:${worker.source.line}`;
        button.append(name, source);
        button.addEventListener("click", () => {
            state.viewMode = "worker";
            state.selectedWorkerId = worker.id;
            state.selectedNodeId = null;
            renderAll();
        });
        elements.workerList.append(button);
    }
}

function renderDefinitionList() {
    clear(elements.definitionList);
    if (!state.document) return;
    const query = elements.definitionSearch.value.trim().toLowerCase();
    const definitions = state.document.definitions.filter(definition => {
        const haystack = [
            definition.displayName,
            definition.flowType,
            definition.source.file
        ].filter(Boolean).join(" ").toLowerCase();
        return haystack.includes(query);
    });

    for (const definition of definitions) {
        const button = el("button", "definition-item");
        button.type = "button";
        button.setAttribute("role", "listitem");
        if (state.viewMode === "definition"
            && definition.id === state.selectedDefinitionId) {
            button.classList.add("active");
        }

        const name = el("span", "definition-name");
        const label = el("span");
        label.textContent = definition.displayName;
        name.append(label);
        if (definition.completeness === "PARTIAL_DYNAMIC") {
            const dot = el("i", "partial-dot");
            dot.title = t("definition.partial");
            name.append(dot);
        }
        const source = el("span", "definition-meta");
        source.textContent = `${definition.source.file}:${definition.source.line}`;
        button.append(name, source);
        button.addEventListener("click", () => {
            state.viewMode = "definition";
            state.selectedDefinitionId = definition.id;
            state.selectedNodeId = null;
            renderAll();
        });
        elements.definitionList.append(button);
    }

    if (definitions.length === 0) {
        const empty = el("div", "list-empty");
        empty.textContent = t("definition.noResults");
        elements.definitionList.append(empty);
    }
}

function renderSelectedDefinition() {
    if (state.viewMode === "map") {
        const hasDefinitions = Boolean(state.document?.definitions.length);
        elements.graphEmpty.hidden = hasDefinitions;
        elements.graphScroll.hidden = !hasDefinitions;
        elements.flowTitle.textContent = t("graph.projectFlowMap");
        elements.flowKind.textContent = "STATIC";
        elements.flowCompleteness.textContent =
            `${state.document.relations?.length || 0} submits`;
        elements.flowSource.textContent = t("graph.flowMapDescription");
        if (hasDefinitions) renderGraph();
        renderNodeDetails();
        renderNotices(state.document.notices || []);
        return;
    }

    if (state.viewMode === "worker") {
        const worker = selectedWorker();
        const hasWorker = Boolean(worker);
        elements.graphEmpty.hidden = hasWorker;
        elements.graphScroll.hidden = !hasWorker;
        if (!worker) {
            elements.flowTitle.textContent = t("graph.selectFlow");
            elements.flowKind.textContent = "";
            elements.flowCompleteness.textContent = "";
            elements.flowSource.textContent = "";
            renderNodeDetails();
            renderNotices([]);
            return;
        }

        const graphData = workerFlowMapData(worker);
        const connectedCount = graphData.nodes.filter(
            node => node.nodeKind === "flow-definition" || node.nodeKind === "linked-flow").length;
        elements.flowTitle.textContent = worker.name || worker.nameExpression;
        elements.flowKind.textContent = worker.kind === "EVENT_WORKER" ? "EventWorker" : "Worker";
        elements.flowCompleteness.textContent = t("graph.connectedFlows", {count: connectedCount});
        elements.flowSource.textContent =
            `${worker.source.file}:${worker.source.line} · ${t("graph.workerFlowMapDescription")}`;
        renderGraph();
        renderNodeDetails();
        renderNotices(state.document.notices || []);
        return;
    }

    const definition = selectedDefinition();
    const hasDefinition = Boolean(definition);
    elements.graphEmpty.hidden = hasDefinition;
    elements.graphScroll.hidden = !hasDefinition;

    if (!definition) {
        elements.flowTitle.textContent = t("graph.selectFlow");
        elements.flowKind.textContent = "";
        elements.flowCompleteness.textContent = "";
        elements.flowSource.textContent = "";
        renderNodeDetails();
        renderNotices([]);
        return;
    }

    elements.flowTitle.textContent = definition.displayName;
    elements.flowKind.textContent = definition.kind === "EVENT_FLOW" ? "EventFlow" : "Flow";
    elements.flowCompleteness.textContent = definition.completeness === "PARTIAL_DYNAMIC"
        ? t("graph.partialAnalysis")
        : t("graph.staticAnalysis");
    elements.flowSource.textContent =
        `${definition.source.file}:${definition.source.line} · graph ${definition.graphHash.slice(0, 10)}`;

    renderGraph();
    renderNodeDetails();
    renderNotices(definition.notices);
}

function renderGraph() {
    clear(elements.graph);
    const graphData = currentGraphData();
    if (!graphData) return;

    const graphNodes = graphData.nodes;
    const graphEdges = graphData.edges;
    addTerminalAndUnknownNodes(graphNodes, graphEdges);
    ensureDefaultPositions(graphData.positionKey, graphNodes, graphEdges);

    const positions = positionsForKey(graphData.positionKey);
    const zoom = zoomForKey(graphData.positionKey);
    const viewportWidth = elements.graphScroll.clientWidth || 760;
    const viewportHeight = elements.graphScroll.clientHeight || 540;
    const bounds = graphNodes.reduce((result, node) => {
        const position = positions.get(node.id);
        result.width = Math.max(result.width, position.x + NODE_WIDTH + 70);
        result.height = Math.max(result.height, position.y + NODE_HEIGHT + 70);
        return result;
    }, {width: viewportWidth / zoom, height: viewportHeight / zoom});

    elements.graph.setAttribute("viewBox", `0 0 ${bounds.width} ${bounds.height}`);
    elements.graph.setAttribute("width", String(bounds.width * zoom));
    elements.graph.setAttribute("height", String(bounds.height * zoom));
    appendMarkers(elements.graph);
    if (state.viewMode === "map") {
        renderMapGuides(elements.graph, graphNodes, positions, bounds.width);
    }
    assignRelationPorts(graphEdges, positions);

    const edgeLayer = svg("g");
    edgeLayer.setAttribute("class", "edge-layer");
    elements.graph.append(edgeLayer);
    for (const edge of graphEdges) {
        renderEdge(edgeLayer, edge, positions);
    }

    const nodeLayer = svg("g");
    nodeLayer.setAttribute("class", "node-layer");
    elements.graph.append(nodeLayer);
    for (const node of graphNodes) {
        renderNode(nodeLayer, node, positions.get(node.id));
    }
}

function renderMapGuides(svgElement, nodes, positions, width) {
    const guideLayer = svg("g");
    guideLayer.setAttribute("class", "map-guide-layer");

    const relationTitle = svg("text");
    relationTitle.setAttribute("class", "map-guide-title");
    relationTitle.setAttribute("x", "64");
    relationTitle.setAttribute("y", "35");
    relationTitle.textContent = "FLOW RELATIONS";
    guideLayer.append(relationTitle);

    const isolated = nodes.filter(node => node.isolated && positions.has(node.id));
    if (isolated.length > 0) {
        const isolatedTop = Math.min(...isolated.map(node => positions.get(node.id).y));
        const lineY = isolatedTop - 68;
        const line = svg("line");
        line.setAttribute("class", "map-guide-line");
        line.setAttribute("x1", "64");
        line.setAttribute("x2", String(Math.max(64, width - 64)));
        line.setAttribute("y1", String(lineY));
        line.setAttribute("y2", String(lineY));
        guideLayer.append(line);

        const isolatedTitle = svg("text");
        isolatedTitle.setAttribute("class", "map-guide-title muted");
        isolatedTitle.setAttribute("x", "64");
        isolatedTitle.setAttribute("y", String(lineY + 28));
        isolatedTitle.textContent = t("graph.otherDefinitions", {count: isolated.length});
        guideLayer.append(isolatedTitle);
    }
    svgElement.append(guideLayer);
}

function assignRelationPorts(edges, positions) {
    const relationEdges = edges.filter(isSubmissionEdge);
    const outgoing = new Map();
    const incoming = new Map();
    for (const edge of relationEdges) {
        if (!outgoing.has(edge.fromStepId)) outgoing.set(edge.fromStepId, []);
        if (!incoming.has(edge.toStepId)) incoming.set(edge.toStepId, []);
        outgoing.get(edge.fromStepId).push(edge);
        incoming.get(edge.toStepId).push(edge);
    }

    for (const grouped of outgoing.values()) {
        grouped.sort((left, right) =>
            (positions.get(left.toStepId)?.y || 0) - (positions.get(right.toStepId)?.y || 0)
            || String(left.id).localeCompare(String(right.id)));
        grouped.forEach((edge, index) => {
            edge.fromPortY = NODE_HEIGHT * (index + 1) / (grouped.length + 1);
            edge.fromPortX = NODE_WIDTH * (index + 1) / (grouped.length + 1);
        });
    }
    for (const grouped of incoming.values()) {
        grouped.sort((left, right) =>
            (positions.get(left.fromStepId)?.y || 0) - (positions.get(right.fromStepId)?.y || 0)
            || String(left.id).localeCompare(String(right.id)));
        grouped.forEach((edge, index) => {
            edge.toPortY = NODE_HEIGHT * (index + 1) / (grouped.length + 1);
            edge.toPortX = NODE_WIDTH * (index + 1) / (grouped.length + 1);
        });
    }
    relationEdges
        .sort((left, right) =>
            (positions.get(left.fromStepId)?.x || 0) - (positions.get(right.fromStepId)?.x || 0)
            || (positions.get(left.fromStepId)?.y || 0) - (positions.get(right.fromStepId)?.y || 0)
            || String(left.id).localeCompare(String(right.id)))
        .forEach((edge, index) => {
            edge.routeOrdinal = index;
        });
}

function currentGraphData() {
    if (state.viewMode === "map") {
        return projectFlowMapData();
    }
    if (state.viewMode === "worker") {
        const worker = selectedWorker();
        return worker ? workerFlowMapData(worker) : null;
    }
    const definition = selectedDefinition();
    return definition ? definitionGraphData(definition) : null;
}

function workerFlowMapData(worker) {
    const relations = (state.document.workerRelations || []).filter(relation =>
        relation.workerDefinitionId === worker.id
        || (!relation.workerDefinitionId && worker.name && relation.workerName === worker.name));
    const rootNode = {
        id: worker.id,
        label: worker.name || worker.nameExpression,
        origin: "worker-definition",
        nodeKind: "worker-definition",
        dynamic: worker.dynamic,
        stepType: worker.kind === "EVENT_WORKER" ? "EventWorker" : "Worker",
        behaviors: [],
        source: worker.source,
        worker,
        relations
    };
    const nodes = [rootNode];
    const groupedTargets = new Map();

    for (const relation of relations) {
        const targetDefinition = relation.toDefinitionId
            ? state.document.definitions.find(item => item.id === relation.toDefinitionId)
            : null;
        const targetKey = targetDefinition
            ? `definition:${targetDefinition.id}`
            : `unresolved:${relation.targetLabel}|${relation.targetExpression}`;
        if (!groupedTargets.has(targetKey)) {
            groupedTargets.set(targetKey, {
                id: `__worker_flow__${targetKey}`,
                label: targetDefinition?.displayName || relation.targetLabel,
                origin: targetDefinition ? "flow-definition" : "unresolved-flow",
                nodeKind: targetDefinition ? "flow-definition" : "linked-flow",
                targetDefinitionId: targetDefinition?.id || null,
                dynamic: !targetDefinition,
                stepType: targetDefinition
                    ? t("graph.openDefinition", {count: targetDefinition.steps.length})
                    : t("graph.targetUnknown"),
                behaviors: [],
                source: targetDefinition?.source || relation.source,
                definition: targetDefinition || null,
                relations: []
            });
        }
        const targetNode = groupedTargets.get(targetKey);
        targetNode.relations.push(relation);
        if (relation.certainty === "PARTIAL") targetNode.dynamic = true;
    }
    nodes.push(...groupedTargets.values());

    const edges = [...groupedTargets.values()].map(targetNode => ({
        id: `worker:${worker.id}:${targetNode.id}`,
        fromStepId: worker.id,
        toStepId: targetNode.id,
        kind: "WORKER_SUBMIT",
        certainty: targetNode.relations.some(relation => relation.certainty === "PARTIAL")
            ? "PARTIAL"
            : "SOURCE_LITERAL",
        label: targetNode.relations.length === 1
            ? relationLabel(targetNode.relations[0])
            : t("relation.callSites", {count: targetNode.relations.length}),
        origin: "relation",
        relations: targetNode.relations
    }));
    return {nodes, edges, positionKey: `__worker__${worker.id}`};
}

function definitionGraphData(definition) {
    const nodes = definition.steps.map(step => ({
        ...step,
        origin: "source",
        label: step.dynamic ? step.idExpression : step.id
    }));
    const edges = definition.transitions.map(transition => ({
        ...transition,
        origin: "source"
    }));
    const linkedNodes = new Map();
    const relations = (state.document.relations || []).filter(
        relation => relation.fromDefinitionId === definition.id);

    for (const relation of relations) {
        const targetDefinition = relation.toDefinitionId
            ? state.document.definitions.find(item => item.id === relation.toDefinitionId)
            : null;
        const targetKey = relation.toDefinitionId
            ? `definition:${relation.toDefinitionId}`
            : `unresolved:${relation.id}`;
        const nodeId = `__linked_flow__${targetKey}`;
        let node = linkedNodes.get(targetKey);
        if (!node) {
            node = {
                id: nodeId,
                label: targetDefinition?.displayName || relation.targetLabel,
                origin: targetDefinition ? "linked-flow" : "unresolved-flow",
                nodeKind: "linked-flow",
                targetDefinitionId: targetDefinition?.id || null,
                dynamic: !targetDefinition,
                stepType: targetDefinition
                    ? t("graph.openDefinition", {count: targetDefinition.steps.length})
                    : t("graph.targetUnknown"),
                behaviors: [],
                source: relation.source,
                relations: []
            };
            linkedNodes.set(targetKey, node);
            nodes.push(node);
        }
        node.relations.push(relation);
        if (relation.certainty === "PARTIAL") node.dynamic = true;
        edges.push({
            id: relation.id,
            fromStepId: relation.fromStepId,
            toStepId: nodeId,
            kind: "FLOW_SUBMIT",
            evidence: "STEP_SOURCE",
            certainty: relation.certainty === "PARTIAL" ? "PARTIAL" : "SOURCE_LITERAL",
            label: relationLabel(relation),
            origin: "relation",
            relation
        });
    }
    return {nodes, edges, positionKey: definition.id};
}

function projectFlowMapData() {
    const nodes = state.document.definitions.map(definition => ({
        id: definition.id,
        label: definition.displayName,
        origin: "flow-definition",
        nodeKind: "flow-definition",
        targetDefinitionId: definition.id,
        dynamic: definition.completeness === "PARTIAL_DYNAMIC",
        stepType: `${definition.steps.length} Steps`,
        behaviors: [],
        source: definition.source,
        definition
    }));
    const nodeIds = new Set(nodes.map(node => node.id));
    const groupedEdges = new Map();

    for (const relation of state.document.relations || []) {
        let targetId = relation.toDefinitionId;
        if (!targetId || !nodeIds.has(targetId)) {
            targetId = `__map_unresolved__${relation.id}`;
            if (!nodeIds.has(targetId)) {
                nodeIds.add(targetId);
                nodes.push({
                    id: targetId,
                    label: relation.targetLabel,
                    origin: "unresolved-flow",
                    nodeKind: "linked-flow",
                    targetDefinitionId: null,
                    dynamic: true,
                    stepType: t("graph.targetUnknown"),
                    behaviors: [],
                    source: relation.source,
                    relations: []
                });
            }
        }
        const targetNode = nodes.find(node => node.id === targetId);
        if (targetNode?.relations) targetNode.relations.push(relation);
        const edgeKey = `${relation.fromDefinitionId}|${targetId}`;
        if (!groupedEdges.has(edgeKey)) {
            groupedEdges.set(edgeKey, {
                id: `map:${edgeKey}`,
                fromStepId: relation.fromDefinitionId,
                toStepId: targetId,
                kind: "FLOW_SUBMIT",
                certainty: relation.certainty === "PARTIAL" ? "PARTIAL" : "SOURCE_LITERAL",
                origin: "relation",
                relations: []
            });
        }
        const edge = groupedEdges.get(edgeKey);
        edge.relations.push(relation);
        if (relation.certainty === "PARTIAL") edge.certainty = "PARTIAL";
    }

    const edges = [...groupedEdges.values()];
    for (const edge of edges) {
        edge.label = edge.relations.length === 1
            ? relationLabel(edge.relations[0])
            : t("relation.callSites", {count: edge.relations.length});
    }
    const connectedNodeIds = new Set(edges.flatMap(edge => [
        edge.fromStepId,
        edge.toStepId
    ]));
    nodes.forEach(node => {
        node.isolated = !connectedNodeIds.has(node.id);
    });
    return {nodes, edges, positionKey: "__project_flow_map__"};
}

function relationLabel(relation) {
    return switchValue(relation.cardinality, {
        CONDITIONAL: t("relation.conditional"),
        ZERO_OR_MANY: "submits · 0..N"
    }, "submits");
}

function isSubmissionEdge(edge) {
    return edge.kind === "FLOW_SUBMIT" || edge.kind === "WORKER_SUBMIT";
}

function switchValue(value, values, fallback) {
    return Object.prototype.hasOwnProperty.call(values, value) ? values[value] : fallback;
}

function addTerminalAndUnknownNodes(nodes, edges) {
    const ids = new Set(nodes.map(node => node.id));
    for (const edge of edges) {
        if (!edge.toStepId && (edge.kind === "FINISH" || edge.kind === "FAIL")) {
            edge.toStepId = `__terminal_${edge.kind.toLowerCase()}`;
            if (!ids.has(edge.toStepId)) {
                ids.add(edge.toStepId);
                nodes.push({
                    id: edge.toStepId,
                    label: edge.kind.toLowerCase(),
                    origin: "terminal",
                    dynamic: false,
                    stepType: "terminal",
                    behaviors: []
                });
            }
        } else if (edge.toStepId && !ids.has(edge.toStepId)) {
            const unknownId = `__unknown_${edge.toStepId}`;
            edge.toStepId = unknownId;
            if (!ids.has(unknownId)) {
                ids.add(unknownId);
                nodes.push({
                    id: unknownId,
                    label: `? ${unknownId.replace("__unknown_", "")}`,
                    origin: "terminal",
                    dynamic: true,
                    stepType: "unresolved target",
                    behaviors: []
                });
            }
        }
    }
}

function ensureDefaultPositions(positionKey, nodes, edges) {
    if (!state.positionsByGraph.has(positionKey)) {
        state.positionsByGraph.set(positionKey, new Map());
    }
    const positions = state.positionsByGraph.get(positionKey);
    if (positionKey === "__project_flow_map__" || positionKey.startsWith("__worker__")) {
        const signature = [
            ...nodes.map(node => node.id),
            ...edges.map(edge => `${edge.fromStepId}->${edge.toStepId}`)
        ].sort().join("|");
        if (state.layoutSignatureByKey.get(positionKey) !== signature) {
            positions.clear();
            if (positionKey.startsWith("__worker__")) {
                layoutWorkerMap(nodes, positions);
            } else {
                layoutFlowMap(nodes, edges, positions);
            }
            state.layoutSignatureByKey.set(positionKey, signature);
        }
        return;
    }

    const activeNodeIds = new Set(nodes.map(node => node.id));
    for (const positionedNodeId of positions.keys()) {
        if (!activeNodeIds.has(positionedNodeId)) {
            positions.delete(positionedNodeId);
        }
    }
    if (positions.size === 0) {
        layoutDefinitionGraph(nodes, edges, positions);
        return;
    }

    const occupied = [...positions.values()];
    nodes.forEach(node => {
        if (positions.has(node.id)) return;
        const position = suggestedPosition(node, edges, positions);
        while (occupied.some(existing =>
            Math.abs(existing.x - position.x) < NODE_WIDTH + 24
            && Math.abs(existing.y - position.y) < NODE_HEIGHT + 24)) {
            position.y += NODE_HEIGHT + 60;
        }
        positions.set(node.id, position);
        occupied.push(position);
    });
}

function layoutDefinitionGraph(nodes, edges, positions) {
    const horizontalGap = 250;
    const verticalStep = 112;
    const sourceNodes = nodes.filter(node => node.nodeKind !== "linked-flow");
    const linkedNodes = nodes.filter(node => node.nodeKind === "linked-flow");

    sourceNodes.forEach((node, index) => {
        positions.set(node.id, {
            x: 64 + index * horizontalGap,
            y: 72 + Math.max(0, index - 2) * verticalStep
        });
    });

    const occupied = [...positions.values()];
    linkedNodes.forEach(node => {
        const position = suggestedPosition(node, edges, positions);
        while (occupied.some(existing =>
            Math.abs(existing.x - position.x) < NODE_WIDTH + 24
            && Math.abs(existing.y - position.y) < NODE_HEIGHT + 24)) {
            position.x += horizontalGap;
        }
        positions.set(node.id, position);
        occupied.push(position);
    });
}

function suggestedPosition(node, edges, positions) {
    const incoming = edges.find(edge =>
        edge.toStepId === node.id && positions.has(edge.fromStepId));
    if (incoming) {
        const from = positions.get(incoming.fromStepId);
        return isSubmissionEdge(incoming)
            ? {x: from.x + 500, y: from.y}
            : {x: from.x + 250, y: from.y + 112};
    }
    return {x: 64, y: 72 + positions.size * (NODE_HEIGHT + 60)};
}

function layoutWorkerMap(nodes, positions) {
    const workerNode = nodes.find(node => node.nodeKind === "worker-definition");
    const flowNodes = nodes
        .filter(node => node !== workerNode)
        .sort((left, right) => String(left.label).localeCompare(String(right.label)));
    const verticalGap = 150;

    if (workerNode) {
        positions.set(workerNode.id, {
            x: 64,
            y: 64
        });
    }
    flowNodes.forEach((node, index) => {
        positions.set(node.id, {
            x: 500,
            y: 64 + index * verticalGap
        });
    });
}

function layoutFlowMap(nodes, edges, positions) {
    const nodesById = new Map(nodes.map(node => [node.id, node]));
    const relationEdges = edges.filter(edge =>
        isSubmissionEdge(edge)
        && edge.fromStepId !== edge.toStepId
        && nodesById.has(edge.fromStepId)
        && nodesById.has(edge.toStepId));
    const connectedIds = new Set(relationEdges.flatMap(edge => [
        edge.fromStepId,
        edge.toStepId
    ]));
    const outgoing = new Map();
    const incoming = new Map();
    const indegree = new Map();
    for (const id of connectedIds) {
        outgoing.set(id, []);
        incoming.set(id, []);
        indegree.set(id, 0);
    }
    for (const edge of relationEdges) {
        outgoing.get(edge.fromStepId).push(edge.toStepId);
        incoming.get(edge.toStepId).push(edge.fromStepId);
        indegree.set(edge.toStepId, indegree.get(edge.toStepId) + 1);
    }

    const labelFor = id => nodesById.get(id)?.label || id;
    const queue = [...connectedIds]
        .filter(id => indegree.get(id) === 0)
        .sort((left, right) => labelFor(left).localeCompare(labelFor(right)));
    const layers = new Map([...connectedIds].map(id => [id, 0]));
    const processed = new Set();
    while (queue.length > 0) {
        const id = queue.shift();
        processed.add(id);
        for (const targetId of outgoing.get(id)) {
            layers.set(targetId, Math.max(
                layers.get(targetId),
                layers.get(id) + 1));
            indegree.set(targetId, indegree.get(targetId) - 1);
            if (indegree.get(targetId) === 0) {
                queue.push(targetId);
                queue.sort((left, right) => labelFor(left).localeCompare(labelFor(right)));
            }
        }
    }
    for (const id of connectedIds) {
        if (!processed.has(id)) layers.set(id, 0);
    }

    const maxLayer = Math.max(0, ...layers.values());
    const layerGroups = Array.from({length: maxLayer + 1}, () => []);
    for (const id of connectedIds) {
        layerGroups[layers.get(id)].push(id);
    }
    layerGroups.forEach(group =>
        group.sort((left, right) => labelFor(left).localeCompare(labelFor(right))));

    for (let pass = 0; pass < 4; pass++) {
        let order = layerOrder(layerGroups);
        for (let layer = 1; layer <= maxLayer; layer++) {
            sortLayerByNeighbors(layerGroups[layer], incoming, order, labelFor);
            order = layerOrder(layerGroups);
        }
        for (let layer = maxLayer - 1; layer >= 0; layer--) {
            sortLayerByNeighbors(layerGroups[layer], outgoing, order, labelFor);
            order = layerOrder(layerGroups);
        }
    }

    const connectedTop = 74;
    const horizontalGap = 272;
    const verticalGap = 150;
    const hubIds = new Set([...connectedIds].filter(id =>
        incoming.get(id).length >= 3));
    const maxHubRows = Math.max(0, ...layerGroups.map(group =>
        group.filter(id => hubIds.has(id)).length));
    const regularGroups = layerGroups.map(group =>
        group.filter(id => !hubIds.has(id)));
    const maxRegularRows = Math.max(1, ...regularGroups.map(group => group.length));
    const regularTop = connectedTop + maxHubRows * verticalGap;

    layerGroups.forEach((group, layer) => {
        const hubs = group.filter(id => hubIds.has(id));
        hubs.forEach((id, index) => {
            positions.set(id, {
                x: 64 + layer * horizontalGap,
                y: connectedTop + index * verticalGap
            });
        });

        const regular = group.filter(id => !hubIds.has(id));
        const centeredOffset = (maxRegularRows - regular.length) * verticalGap / 2;
        regular.forEach((id, index) => {
            positions.set(id, {
                x: 64 + layer * horizontalGap,
                y: regularTop + centeredOffset + index * verticalGap
            });
        });
    });

    const connectedBottom = connectedIds.size === 0
        ? connectedTop
        : Math.max(...[...connectedIds].map(id => positions.get(id).y + NODE_HEIGHT));
    const isolated = nodes
        .filter(node => !connectedIds.has(node.id))
        .sort((left, right) => String(left.label).localeCompare(String(right.label)));
    const isolatedTop = connectedBottom + 150;
    const isolatedColumns = Math.min(4, Math.max(3, maxLayer + 1));
    isolated.forEach((node, index) => {
        positions.set(node.id, {
            x: 64 + (index % isolatedColumns) * 250,
            y: isolatedTop + Math.floor(index / isolatedColumns) * 140
        });
    });
}

function layerOrder(layerGroups) {
    const result = new Map();
    for (const group of layerGroups) {
        group.forEach((id, index) => result.set(id, index));
    }
    return result;
}

function sortLayerByNeighbors(group, neighbors, order, labelFor) {
    const barycenter = id => {
        const adjacent = neighbors.get(id) || [];
        if (adjacent.length === 0) return Number.POSITIVE_INFINITY;
        return adjacent.reduce((sum, neighborId) =>
            sum + (order.get(neighborId) || 0), 0) / adjacent.length;
    };
    group.sort((left, right) =>
        barycenter(left) - barycenter(right)
        || labelFor(left).localeCompare(labelFor(right)));
}

function renderEdge(layer, edge, positions) {
    const from = positions.get(edge.fromStepId);
    const to = positions.get(edge.toStepId);
    if (!from || !to) return;

    const anchors = edgeAnchors(from, to, edge);
    const start = anchors.start;
    const end = anchors.end;
    let pathData;
    let labelPoint;

    if (isSubmissionEdge(edge)) {
        if (anchors.axis === "horizontal") {
            if (Math.abs(end.y - start.y) < 2) {
                pathData = `M ${start.x} ${start.y} H ${end.x}`;
                labelPoint = {x: (start.x + end.x) / 2, y: start.y - 8};
            } else {
                const middle = (start.x + end.x) / 2;
                pathData = `M ${start.x} ${start.y} H ${middle} V ${end.y} H ${end.x}`;
                labelPoint = edge.kind === "WORKER_SUBMIT"
                    ? {x: (middle + end.x) / 2, y: end.y - 8}
                    : {x: middle, y: Math.min(start.y, end.y) - 8};
            }
        } else {
            const middle = (start.y + end.y) / 2;
            pathData = `M ${start.x} ${start.y} V ${middle} H ${end.x} V ${end.y}`;
            labelPoint = {x: (start.x + end.x) / 2, y: middle - 8};
        }
    } else if (anchors.axis === "vertical") {
        const direction = end.y >= start.y ? 1 : -1;
        const control = Math.max(34, Math.abs(end.y - start.y) / 2);
        pathData = `M ${start.x} ${start.y} C ${start.x} ${start.y + direction * control}, ${end.x} ${end.y - direction * control}, ${end.x} ${end.y}`;
        labelPoint = {x: (start.x + end.x) / 2 + 14, y: (start.y + end.y) / 2 - 4};
    } else {
        const direction = end.x >= start.x ? 1 : -1;
        const control = Math.max(34, Math.abs(end.x - start.x) / 2);
        pathData = `M ${start.x} ${start.y} C ${start.x + direction * control} ${start.y}, ${end.x - direction * control} ${end.y}, ${end.x} ${end.y}`;
        labelPoint = {x: (start.x + end.x) / 2, y: (start.y + end.y) / 2 - 7};
    }

    const path = svg("path");
    const classes = ["edge-path"];
    if (String(edge.kind).toLowerCase().includes("go")) classes.push("goto");
    if (isSubmissionEdge(edge)) classes.push("submits");
    if (edge.certainty === "PARTIAL") classes.push("partial");
    path.setAttribute("class", classes.join(" "));
    path.setAttribute("d", pathData);
    const marker = edge.origin === "relation"
        ? "url(#arrow-relation)"
        : "url(#arrow-source)";
    path.setAttribute("marker-end", marker);
    if (edge.relations?.length) {
        const title = svg("title");
        const callSites = [...new Set(edge.relations.map(relation =>
            relation.fromStepId || `${relation.source.file}:${relation.source.line}`))];
        title.textContent = `${edge.label || "submits"} · ${callSites.join(", ")}`;
        path.append(title);
    }
    layer.append(path);

    const label = svg("text");
    label.setAttribute("class", "edge-label");
    label.setAttribute("x", String(labelPoint.x));
    label.setAttribute("y", String(labelPoint.y));
    label.setAttribute("text-anchor", "middle");
    label.textContent = edge.label || edge.kind;
    layer.append(label);
}

function edgeAnchors(from, to, edge) {
    const fromCenter = {
        x: from.x + NODE_WIDTH / 2,
        y: from.y + NODE_HEIGHT / 2
    };
    const toCenter = {
        x: to.x + NODE_WIDTH / 2,
        y: to.y + NODE_HEIGHT / 2
    };
    const horizontalGap = Math.max(0, Math.abs(toCenter.x - fromCenter.x) - NODE_WIDTH);
    const verticalGap = Math.max(0, Math.abs(toCenter.y - fromCenter.y) - NODE_HEIGHT);

    if (edge.kind === "WORKER_SUBMIT") {
        const goingRight = toCenter.x >= fromCenter.x;
        return {
            axis: "horizontal",
            start: {
                x: goingRight ? from.x + NODE_WIDTH : from.x,
                y: from.y + (edge.fromPortY ?? NODE_HEIGHT / 2)
            },
            end: {
                x: goingRight ? to.x : to.x + NODE_WIDTH,
                y: to.y + (edge.toPortY ?? NODE_HEIGHT / 2)
            }
        };
    }

    if (verticalGap > horizontalGap) {
        const goingDown = toCenter.y >= fromCenter.y;
        return {
            axis: "vertical",
            start: {
                x: from.x + (edge.fromPortX ?? NODE_WIDTH / 2),
                y: goingDown ? from.y + NODE_HEIGHT : from.y
            },
            end: {
                x: to.x + (edge.toPortX ?? NODE_WIDTH / 2),
                y: goingDown ? to.y : to.y + NODE_HEIGHT
            }
        };
    }

    const goingRight = toCenter.x >= fromCenter.x;
    return {
        axis: "horizontal",
        start: {
            x: goingRight ? from.x + NODE_WIDTH : from.x,
            y: from.y + (edge.fromPortY ?? NODE_HEIGHT / 2)
        },
        end: {
            x: goingRight ? to.x : to.x + NODE_WIDTH,
            y: to.y + (edge.toPortY ?? NODE_HEIGHT / 2)
        }
    };
}

function renderNode(layer, node, position) {
    const group = svg("g");
    const classes = ["graph-node", node.origin];
    if (node.dynamic) classes.push("dynamic");
    if (node.isolated) classes.push("isolated");
    if (node.id === state.selectedNodeId) classes.push("selected");
    group.setAttribute("class", classes.join(" "));
    group.setAttribute("transform", `translate(${position.x}, ${position.y})`);
    group.dataset.nodeId = node.id;

    const card = svg("rect");
    card.setAttribute("class", "node-card");
    card.setAttribute("width", String(NODE_WIDTH));
    card.setAttribute("height", String(NODE_HEIGHT));
    card.setAttribute("rx", "13");
    group.append(card);

    const kicker = svg("text");
    kicker.setAttribute("class", "node-kicker");
    kicker.setAttribute("x", "14");
    kicker.setAttribute("y", "20");
    kicker.textContent = switchValue(node.origin, {
        terminal: "TERMINAL",
        "worker-definition": "SOURCE WORKER",
        "linked-flow": "LINKED FLOW",
        "unresolved-flow": "POSSIBLE FLOW",
        "flow-definition": "FLOW DEFINITION"
    }, node.dynamic ? "PARTIAL STEP" : "SOURCE STEP");
    group.append(kicker);

    const title = svg("text");
    title.setAttribute("class", "node-title");
    title.setAttribute("x", "14");
    title.setAttribute("y", "43");
    title.textContent = truncate(node.label || node.id, 22);
    group.append(title);

    const type = svg("text");
    type.setAttribute("class", "node-type");
    type.setAttribute("x", "14");
    type.setAttribute("y", "62");
    type.textContent = truncate(node.stepType || node.stepExpression || "unknown implementation", 28);
    group.append(type);

    const badges = [];
    if (node.eventSubscriptions?.length) badges.push(`events ${node.eventSubscriptions.length}`);
    if (node.internalPhases?.length) badges.push(`stepNo ${node.internalPhases.length}`);
    else if (node.internalStructurePartial) badges.push("stepNo ?");
    if (node.guarded) badges.push("guard");
    if (node.durableStep) badges.push("durable");
    if (node.behaviors?.includes("GO_TO")) badges.push("goTo");
    if (node.behaviors?.includes("FINISH")) badges.push("finish");
    if (node.behaviors?.includes("FAIL")) badges.push("fail");
    if (node.behaviors?.includes("REPEAT")) badges.push("repeat");
    if (node.nodeKind === "linked-flow") badges.push("submits");
    let badgeX = 14;
    badges.slice(0, 3).forEach(badge => {
        const width = Math.max(42, Math.min(64, 14 + badge.length * 4.5));
        if (badgeX + width > NODE_WIDTH - 10) return;
        const rect = svg("rect");
        rect.setAttribute("class", "node-badge");
        rect.setAttribute("x", String(badgeX));
        rect.setAttribute("y", "69");
        rect.setAttribute("width", String(width));
        rect.setAttribute("height", "11");
        rect.setAttribute("rx", "5");
        group.append(rect);
        const text = svg("text");
        text.setAttribute("class", "node-badge-text");
        text.setAttribute("x", String(badgeX + width / 2));
        text.setAttribute("y", "77.5");
        text.setAttribute("text-anchor", "middle");
        text.textContent = badge;
        group.append(text);
        badgeX += width + 5;
    });

    group.addEventListener("click", () => {
        if (node.targetDefinitionId
            && (node.nodeKind === "linked-flow" || node.nodeKind === "flow-definition")) {
            state.viewMode = "definition";
            state.selectedDefinitionId = node.targetDefinitionId;
            state.selectedNodeId = null;
            renderAll();
            return;
        }
        state.selectedNodeId = node.id;
        renderGraph();
        renderNodeDetails();
    });
    group.addEventListener("pointerdown", event => {
        const point = clientToSvg(event.clientX, event.clientY);
        state.drag = {
            nodeId: node.id,
            offsetX: point.x - position.x,
            offsetY: point.y - position.y
        };
        event.preventDefault();
    });
    layer.append(group);
}

function appendMarkers(svgElement) {
    const defs = svg("defs");
    defs.append(
        arrowMarker("arrow-source", "#8ba399"),
        arrowMarker("arrow-relation", "#32749d"));
    svgElement.append(defs);
}

function arrowMarker(id, color) {
    const marker = svg("marker");
    marker.setAttribute("id", id);
    marker.setAttribute("viewBox", "0 0 10 10");
    marker.setAttribute("refX", "9");
    marker.setAttribute("refY", "5");
    marker.setAttribute("markerWidth", "6");
    marker.setAttribute("markerHeight", "6");
    marker.setAttribute("orient", "auto-start-reverse");
    const path = svg("path");
    path.setAttribute("d", "M 0 0 L 10 5 L 0 10 z");
    path.setAttribute("fill", color);
    marker.append(path);
    return marker;
}

function renderNodeDetails() {
    const graphData = currentGraphData();
    if (graphData) addTerminalAndUnknownNodes(graphData.nodes, graphData.edges);
    const node = graphData
        ? graphData.nodes.find(item => item.id === state.selectedNodeId)
        : null;
    elements.nodeEmpty.hidden = Boolean(node);
    elements.nodeDetails.hidden = !node;
    clear(elements.nodeDetails);
    if (!node) return;

    const kicker = el("span", "detail-kicker");
    kicker.textContent = switchValue(node.origin, {
        terminal: "TERMINAL",
        "worker-definition": "SOURCE WORKER",
        "linked-flow": "LINKED FLOW",
        "unresolved-flow": "POSSIBLE FLOW",
        "flow-definition": "FLOW DEFINITION"
    }, "SOURCE STEP");
    const title = el("h3", "detail-title");
    title.textContent = node.label || node.id;
    elements.nodeDetails.append(kicker, title);
    const expressionText = meaningfulStepIdExpression(node);
    if (expressionText) {
        const expression = el("p", "detail-expression");
        expression.textContent = expressionText;
        elements.nodeDetails.append(expression);
    } else {
        title.classList.add("without-expression");
    }

    if (node.nodeKind === "worker-definition") {
        const worker = node.worker;
        const relations = node.relations || [];
        const connectedFlows = new Set(relations.map(relation =>
            relation.toDefinitionId || `${relation.targetLabel}|${relation.targetExpression}`));
        const locations = [...new Set(relations.map(relation =>
            `${relation.source.file}:${relation.source.line}`))].join(", ");
        const workerGrid = el("div", "detail-grid");
        workerGrid.append(
            detailRow(t("detail.workerName"), worker.name || worker.nameExpression, true),
            detailRow(t("detail.workerKind"),
                worker.kind === "EVENT_WORKER" ? "EventWorker" : "Worker", false),
            detailRow(t("detail.declaration"),
                `${worker.source.file}:${worker.source.line}`, true),
            detailRow(t("detail.definitionSource"),
                worker.definitionSource === "SPRING_CONFIGURATION"
                    ? "Spring configuration"
                    : "Java builder", false),
            detailRow(t("detail.connectedFlows"), String(connectedFlows.size), false),
            detailRow(t("detail.submissionSites"), locations || t("detail.unknown"), true));
        elements.nodeDetails.append(workerGrid);
        return;
    }

    if (node.nodeKind === "linked-flow") {
        const relations = node.relations || [];
        const certainty = relations.some(relation => relation.certainty === "PARTIAL")
            ? t("detail.partial")
            : t("detail.sourceConfirmed");
        const locations = [...new Set(relations.map(relation =>
            `${relation.source.file}:${relation.source.line}`))].join(", ");
        const submittedBy = [...new Set(relations.map(relation =>
            relation.fromStepId))].join(", ");
        const relationGrid = el("div", "detail-grid");
        relationGrid.append(
            detailRow(t("detail.relation"), t("detail.separateSubmission"), false),
            detailRow(t("detail.submissionStep"), submittedBy || t("detail.unknown"), true),
            detailRow(t("detail.certainty"), certainty, false),
            detailRow(t("detail.callSite"), locations || t("detail.unknown"), true));
        elements.nodeDetails.append(relationGrid);
        return;
    }

    const grid = el("div", "detail-grid");
    grid.append(
        detailRow(t("detail.stepImplementation"),
            node.stepType || node.stepExpression || t("detail.unknown"), true),
        detailRow(t("detail.declaration"), node.source
            ? `${node.source.file}:${node.source.line}`
            : t("detail.unknown"), true),
        detailRow("Guard", node.guarded
            ? (node.guardType || node.guardExpression || t("detail.present"))
            : t("detail.absent"), true),
        detailRow(t("detail.durability"),
            node.durableStep ? "durableStep" : t("detail.regularStep"), false));

    const behaviorRow = el("div", "detail-row");
    const behaviorLabel = el("span");
    behaviorLabel.textContent = t("detail.confirmedResult");
    behaviorRow.append(behaviorLabel);
    const chips = el("div", "behavior-list");
    if (node.behaviors?.length) {
        for (const behavior of node.behaviors) {
            const chip = el("i", "behavior-chip");
            chip.textContent = behavior.toLowerCase();
            chips.append(chip);
        }
    } else {
        const none = el("strong");
        none.textContent = t("detail.notConfirmed");
        chips.append(none);
    }
    behaviorRow.append(chips);
    grid.append(behaviorRow);
    elements.nodeDetails.append(grid);
    renderEventSubscriptions(node, elements.nodeDetails);
    renderInternalPhases(node, elements.nodeDetails);
}

function meaningfulStepIdExpression(node) {
    const expression = typeof node.idExpression === "string"
        ? node.idExpression.trim()
        : "";
    if (!expression || expression === node.id) return null;
    if (expression.startsWith('"') && expression.endsWith('"')) {
        try {
            if (JSON.parse(expression) === node.id) return null;
        } catch (_) {
            // Java and JSON string syntax overlap for normal Step IDs. If this
            // is a more complex Java expression, keep showing it.
        }
    }
    return expression;
}

function renderEventSubscriptions(node, container) {
    const subscriptions = node.eventSubscriptions || [];
    if (!subscriptions.length) return;

    const section = el("section", "source-detail-section");
    const heading = el("h4", "source-detail-heading");
    heading.textContent = `${t("detail.eventSubscriptions")} · ${subscriptions.length}`;
    section.append(heading);
    const list = el("div", "event-subscription-list");
    for (const subscription of subscriptions) {
        const card = el("article", "event-subscription-card");
        const header = el("div", "event-subscription-header");
        const eventName = el("strong");
        eventName.textContent = subscription.eventType || subscription.eventExpression;
        const kind = el("span", "source-fact-chip");
        kind.textContent = subscription.kind === "AWAIT"
            ? t("detail.awaitEvent")
            : t("detail.subscription");
        header.append(eventName, kind);
        if (subscription.filtered) {
            const filtered = el("span", "source-fact-chip muted");
            filtered.textContent = t("detail.filtered");
            header.append(filtered);
        }
        card.append(header);

        const exactClassExpression = subscription.eventType
            ? `${subscription.eventType}.class`
            : null;
        if (!subscription.eventType || subscription.eventExpression !== exactClassExpression) {
            const expression = el("code", "event-expression");
            expression.textContent = subscription.eventExpression;
            card.append(expression);
        }
        if (subscription.emittedSignals?.length) {
            const signals = el("div", "source-fact-list");
            for (const signal of subscription.emittedSignals) {
                const chip = el("span", "signal-chip");
                chip.textContent = `${t("detail.emitsSignal")} · ${signal.name || signal.expression}`;
                signals.append(chip);
            }
            card.append(signals);
        }
        const meta = el("small", "source-fact-meta");
        const source = subscription.source
            ? `${subscription.source.file}:${subscription.source.line}`
            : t("detail.unknown");
        meta.textContent = `${subscription.lifecycleMethod} · ${source}`;
        card.append(meta);
        list.append(card);
    }
    section.append(list);
    container.append(section);
}

function renderInternalPhases(node, container) {
    const phases = node.internalPhases || [];
    if (!phases.length && !node.internalStructurePartial) return;

    const section = el("details", "internal-phase-section");
    const summary = el("summary", "internal-phase-summary");
    summary.textContent = `${t("detail.internalPhases")} · ${phases.length || "?"}`;
    section.append(summary);
    if (node.internalStructurePartial) {
        const partial = el("p", "internal-phase-partial");
        partial.textContent = t("detail.internalPartial");
        section.append(partial);
    }
    if (phases.length) {
        section.append(createInternalPhaseGraph(node));
    }
    container.append(section);
}

function createInternalPhaseGraph(node) {
    const phases = [...(node.internalPhases || [])]
        .sort((left, right) => left.stepNo - right.stepNo);
    const transitions = node.internalTransitions || [];
    const indexByNumber = new Map(phases.map((phase, index) => [phase.stepNo, index]));
    const width = 300;
    const nodeX = 12;
    const nodeWidth = 204;
    const nodeHeight = 58;
    const rowHeight = 84;
    const height = Math.max(92, phases.length * rowHeight + 12);
    const wrapper = el("div", "internal-phase-graph-wrap");
    const graph = svg("svg");
    graph.setAttribute("class", "internal-phase-graph");
    graph.setAttribute("viewBox", `0 0 ${width} ${height}`);
    graph.setAttribute("role", "img");
    graph.setAttribute("aria-label", t("detail.internalPhases"));

    const defs = svg("defs");
    defs.append(arrowMarker("internal-phase-arrow", "#719184"));
    graph.append(defs);
    const edgeLayer = svg("g");
    edgeLayer.setAttribute("class", "internal-phase-edges");
    const nodeLayer = svg("g");
    nodeLayer.setAttribute("class", "internal-phase-nodes");
    graph.append(edgeLayer, nodeLayer);

    transitions.forEach((transition, edgeIndex) => {
        const fromIndex = indexByNumber.get(transition.fromStepNo);
        if (fromIndex === undefined) return;
        const toIndex = transition.toStepNo == null
            ? undefined
            : indexByNumber.get(transition.toStepNo);
        const fromY = 12 + fromIndex * rowHeight;
        const path = svg("path");
        path.setAttribute("class", `internal-phase-edge ${transition.trigger.toLowerCase()}`);
        path.setAttribute("marker-end", "url(#internal-phase-arrow)");
        let labelX;
        let labelY;
        if (toIndex === fromIndex + 1) {
            const centerX = nodeX + nodeWidth / 2;
            const endY = 12 + toIndex * rowHeight;
            path.setAttribute("d", `M ${centerX} ${fromY + nodeHeight} V ${endY}`);
            labelX = centerX + 6;
            labelY = fromY + nodeHeight + 14;
        } else if (toIndex !== undefined) {
            const toY = 12 + toIndex * rowHeight + nodeHeight / 2;
            const trackX = 234 + (edgeIndex % 4) * 11;
            path.setAttribute("d",
                `M ${nodeX + nodeWidth} ${fromY + nodeHeight / 2} H ${trackX} V ${toY} H ${nodeX + nodeWidth}`);
            labelX = trackX + 3;
            labelY = Math.min(fromY + nodeHeight / 2, toY) + 11;
        } else {
            path.setAttribute("d",
                `M ${nodeX + nodeWidth} ${fromY + nodeHeight / 2} H 282`);
            labelX = 222;
            labelY = fromY + nodeHeight / 2 - 5;
        }
        edgeLayer.append(path);
        if (transition.trigger === "TIMEOUT" || toIndex === undefined) {
            const edgeLabel = svg("text");
            edgeLabel.setAttribute("class", "internal-phase-edge-label");
            edgeLabel.setAttribute("x", String(labelX));
            edgeLabel.setAttribute("y", String(labelY));
            edgeLabel.textContent = transition.trigger === "TIMEOUT"
                ? t("detail.transitionTimeout")
                : `? ${truncate(transition.toExpression, 13)}`;
            edgeLayer.append(edgeLabel);
        }
    });

    for (const [index, phase] of phases.entries()) {
        const y = 12 + index * rowHeight;
        const group = svg("g");
        group.setAttribute("class", "internal-phase-node");
        const card = svg("rect");
        card.setAttribute("x", String(nodeX));
        card.setAttribute("y", String(y));
        card.setAttribute("width", String(nodeWidth));
        card.setAttribute("height", String(nodeHeight));
        card.setAttribute("rx", "9");
        group.append(card);
        const title = svg("text");
        title.setAttribute("class", "internal-phase-title");
        title.setAttribute("x", String(nodeX + 11));
        title.setAttribute("y", String(y + 20));
        title.textContent = `${phase.stepNo} · ${phase.label}`;
        group.append(title);

        const facts = [];
        if (phase.startsTimeout) facts.push(t("detail.timeoutStart"));
        if (phase.checksTimeout) facts.push(t("detail.timeoutCheck"));
        const signalNames = [...new Set((phase.signalUses || [])
            .map(signal => signal.name || signal.expression))];
        const eventNames = phaseEventNames(phase, node.eventSubscriptions || []);
        if (eventNames.length) facts.push(`${t("detail.events")} ${eventNames.length}`);
        if (signalNames.length) facts.push(`${t("detail.signals")} ${signalNames.length}`);
        const meta = svg("text");
        meta.setAttribute("class", "internal-phase-meta");
        meta.setAttribute("x", String(nodeX + 11));
        meta.setAttribute("y", String(y + 42));
        meta.textContent = truncate(facts.join(" · ") || "setStepNo phase", 38);
        group.append(meta);
        nodeLayer.append(group);
    }
    wrapper.append(graph);

    const facts = el("div", "internal-phase-facts");
    for (const phase of phases) {
        const signalNames = [...new Set((phase.signalUses || [])
            .map(signal => signal.name || signal.expression))];
        const eventNames = phaseEventNames(phase, node.eventSubscriptions || []);
        if (!signalNames.length && !eventNames.length) continue;
        const row = el("div", "internal-phase-fact-row");
        const label = el("strong");
        label.textContent = phase.label;
        const values = el("span");
        const parts = [];
        if (eventNames.length) parts.push(`${t("detail.events")}: ${eventNames.join(", ")}`);
        if (signalNames.length) parts.push(`${t("detail.signals")}: ${signalNames.join(", ")}`);
        values.textContent = parts.join(" · ");
        row.append(label, values);
        facts.append(row);
    }
    if (facts.childElementCount) wrapper.append(facts);
    return wrapper;
}

function phaseEventNames(phase, subscriptions) {
    const signalKeys = new Set((phase.signalUses || []).flatMap(signal =>
        [signal.name, signal.expression].filter(Boolean)));
    return [...new Set(subscriptions
        .filter(subscription => (subscription.emittedSignals || []).some(signal =>
            signalKeys.has(signal.name) || signalKeys.has(signal.expression)))
        .map(subscription => subscription.eventType || subscription.eventExpression))];
}

function detailRow(label, value, code) {
    const row = el("div", "detail-row");
    const heading = el("span");
    heading.textContent = label;
    const content = el(code ? "code" : "strong");
    content.textContent = value;
    row.append(heading, content);
    return row;
}

function renderNotices(notices) {
    elements.noticeStrip.hidden = notices.length === 0;
    elements.noticeSummary.textContent = notices.length
        ? t("notice.summary", {count: notices.length})
        : "";
    clear(elements.noticeList);
    for (const notice of notices) {
        const item = el("div", "notice-item");
        const source = notice.source
            ? ` · ${notice.source.file}:${notice.source.line}`
            : "";
        item.textContent = `${notice.code}: ${notice.message}${source}`;
        elements.noticeList.append(item);
    }
}

function buildWorkspaceFile() {
    if (!state.document) return null;
    return {
        schemaVersion: WORKSPACE_SCHEMA_VERSION,
        savedAt: new Date().toISOString(),
        sourceSnapshot: {
            kind: "static-source-snapshot",
            document: state.document
        },
        view: {
            viewMode: state.viewMode,
            selectedWorkerId: state.selectedWorkerId,
            selectedDefinitionId: state.selectedDefinitionId,
            selectedNodeId: state.selectedNodeId,
            collapsedSidebarSections: [...state.collapsedSidebarSections],
            positionsByGraph: mapToObject(
                state.positionsByGraph,
                positions => mapToObject(positions, position => ({
                    x: position.x,
                    y: position.y
                }))),
            zoomByGraph: mapToObject(state.zoomByKey),
            layoutSignatureByGraph: mapToObject(state.layoutSignatureByKey)
        }
    };
}

function saveWorkspaceFile() {
    const workspace = buildWorkspaceFile();
    if (!workspace) return;
    const projectName = safeFileName(state.document.project.name || "flower-project");
    downloadJson(workspace, `${projectName}.flower-graph.json`);
}

async function openWorkspaceFile(event) {
    const file = event.target.files?.[0];
    if (!file) return;
    setProjectStatus("loading", "project.loadingLabel", file.name);
    try {
        const workspace = JSON.parse(await file.text());
        restoreWorkspaceFile(workspace, file.name);
        renderAll();
    } catch (error) {
        const message = error instanceof Error && error.message
            ? error.message
            : t("workspace.readFailed");
        setProjectStatus("error", "project.workspaceErrorLabel", message);
    } finally {
        elements.workspaceFileInput.value = "";
    }
}

function restoreWorkspaceFile(workspace, fileName) {
    validateWorkspaceFile(workspace);
    const documentSnapshot = workspace.sourceSnapshot.document;
    const view = workspace.view || {};
    const definitionIds = new Set(documentSnapshot.definitions.map(definition => definition.id));
    documentSnapshot.workers = Array.isArray(documentSnapshot.workers)
        ? documentSnapshot.workers
        : [];
    documentSnapshot.workerRelations = Array.isArray(documentSnapshot.workerRelations)
        ? documentSnapshot.workerRelations
        : [];
    const workerIds = new Set(documentSnapshot.workers.map(worker => worker.id));

    state.document = documentSnapshot;
    state.documentOrigin = "file";
    state.loadedFileName = fileName;
    state.loadedSavedAt = workspace.savedAt;
    state.viewMode = view.viewMode === "map"
        ? "map"
        : view.viewMode === "worker" && workerIds.has(view.selectedWorkerId)
            ? "worker"
            : "definition";
    state.selectedWorkerId = workerIds.has(view.selectedWorkerId)
        ? view.selectedWorkerId
        : null;
    state.selectedDefinitionId = definitionIds.has(view.selectedDefinitionId)
        ? view.selectedDefinitionId
        : documentSnapshot.definitions[0]?.id || null;
    state.selectedNodeId = typeof view.selectedNodeId === "string"
        ? view.selectedNodeId
        : null;
    state.collapsedSidebarSections = new Set(
        Array.isArray(view.collapsedSidebarSections)
            ? view.collapsedSidebarSections.filter(section =>
                section === "workers" || section === "flows")
            : []);
    state.positionsByGraph = mapFromObject(
        view.positionsByGraph,
        restorePositions);
    state.zoomByKey = mapFromObject(
        view.zoomByGraph,
        value => Number.isFinite(Number(value))
            ? Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, Number(value)))
            : undefined);
    state.layoutSignatureByKey = mapFromObject(
        view.layoutSignatureByGraph,
        value => typeof value === "string" ? value : undefined);
    state.drag = null;
    state.pan = null;
    elements.definitionSearch.value = "";
}

function validateWorkspaceFile(workspace) {
    const documentSnapshot = workspace?.sourceSnapshot?.document;
    const validDefinitions = Array.isArray(documentSnapshot?.definitions)
        && documentSnapshot.definitions.every(definition =>
            typeof definition?.id === "string"
            && Array.isArray(definition.steps)
            && Array.isArray(definition.transitions)
            && Array.isArray(definition.notices)
            && typeof definition?.source?.file === "string");
    if (workspace?.schemaVersion !== WORKSPACE_SCHEMA_VERSION
        || workspace?.sourceSnapshot?.kind !== "static-source-snapshot"
        || typeof documentSnapshot?.project?.name !== "string"
        || !validDefinitions
        || !Array.isArray(documentSnapshot.relations)
        || !Array.isArray(documentSnapshot.notices)) {
        throw new Error(t("workspace.invalid"));
    }
}

function restorePositions(value) {
    return mapFromObject(value, position => {
        const x = Number(position?.x);
        const y = Number(position?.y);
        return Number.isFinite(x) && Number.isFinite(y)
            ? {x: Math.max(30, x), y: Math.max(30, y)}
            : undefined;
    });
}

function mapToObject(map, transform = value => value) {
    return Object.fromEntries(
        [...map.entries()].map(([key, value]) => [key, transform(value)]));
}

function mapFromObject(value, transform) {
    const result = new Map();
    if (!value || typeof value !== "object" || Array.isArray(value)) return result;
    for (const [key, item] of Object.entries(value)) {
        const transformed = transform(item);
        if (transformed !== undefined) result.set(key, transformed);
    }
    return result;
}

function downloadJson(value, fileName) {
    const blob = new Blob([JSON.stringify(value, null, 2)], {
        type: "application/json"
    });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement("a");
    anchor.href = url;
    anchor.download = fileName;
    anchor.click();
    URL.revokeObjectURL(url);
}

function selectedDefinition() {
    return state.document?.definitions.find(
        definition => definition.id === state.selectedDefinitionId) || null;
}

function selectedWorker() {
    return state.document?.workers?.find(
        worker => worker.id === state.selectedWorkerId) || null;
}

function positionsForSelected() {
    const key = selectedGraphKey();
    return key ? positionsForKey(key) : new Map();
}

function positionsForKey(key) {
    if (!state.positionsByGraph.has(key)) {
        state.positionsByGraph.set(key, new Map());
    }
    return state.positionsByGraph.get(key);
}

function selectedGraphKey() {
    if (state.viewMode === "map") return "__project_flow_map__";
    if (state.viewMode === "worker") {
        const worker = selectedWorker();
        return worker ? `__worker__${worker.id}` : null;
    }
    return selectedDefinition()?.id || null;
}

function zoomForSelected() {
    const key = selectedGraphKey();
    return key ? zoomForKey(key) : 1;
}

function zoomForKey(key) {
    return state.zoomByKey.get(key) || 1;
}

function zoomFromCenter(factor) {
    setZoomAt(zoomForSelected() * factor);
}

function setZoomAt(requestedZoom, clientX, clientY) {
    const key = selectedGraphKey();
    if (!key || elements.graphScroll.hidden) return;

    const currentZoom = zoomForKey(key);
    const nextZoom = Math.min(MAX_ZOOM, Math.max(MIN_ZOOM, requestedZoom));
    if (Math.abs(nextZoom - currentZoom) < 0.001) {
        updateZoomControls();
        return;
    }

    const scroll = elements.graphScroll;
    const rect = scroll.getBoundingClientRect();
    const anchorClientX = clientX ?? rect.left + rect.width / 2;
    const anchorClientY = clientY ?? rect.top + rect.height / 2;
    const anchorX = Math.min(rect.width, Math.max(0, anchorClientX - rect.left));
    const anchorY = Math.min(rect.height, Math.max(0, anchorClientY - rect.top));
    const logicalX = (scroll.scrollLeft + anchorX) / currentZoom;
    const logicalY = (scroll.scrollTop + anchorY) / currentZoom;

    state.zoomByKey.set(key, nextZoom);
    renderGraph();
    scroll.scrollLeft = logicalX * nextZoom - anchorX;
    scroll.scrollTop = logicalY * nextZoom - anchorY;
    updateZoomControls();
}

function updateZoomControls() {
    const key = selectedGraphKey();
    const enabled = Boolean(key) && !elements.graphScroll.hidden;
    const zoom = key ? zoomForKey(key) : 1;
    elements.zoomReset.textContent = `${Math.round(zoom * 100)}%`;
    elements.zoomReset.disabled = !enabled;
    elements.zoomOut.disabled = !enabled || zoom <= MIN_ZOOM + 0.001;
    elements.zoomIn.disabled = !enabled || zoom >= MAX_ZOOM - 0.001;
}

function clientToSvg(clientX, clientY) {
    const point = elements.graph.createSVGPoint();
    point.x = clientX;
    point.y = clientY;
    const matrix = elements.graph.getScreenCTM();
    return matrix ? point.matrixTransform(matrix.inverse()) : point;
}

function applyTranslations() {
    document.querySelectorAll("[data-i18n]").forEach(node => {
        node.textContent = t(node.dataset.i18n);
    });
    document.querySelectorAll("[data-i18n-placeholder]").forEach(node => {
        node.setAttribute("placeholder", t(node.dataset.i18nPlaceholder));
    });
    document.querySelectorAll("[data-i18n-title]").forEach(node => {
        node.setAttribute("title", t(node.dataset.i18nTitle));
    });
    document.querySelectorAll("[data-i18n-aria-label]").forEach(node => {
        node.setAttribute("aria-label", t(node.dataset.i18nAriaLabel));
    });
}

function t(key, values = {}) {
    const template = MESSAGES[locale][key] ?? MESSAGES.en[key] ?? key;
    return Object.entries(values).reduce(
        (message, [name, value]) => message.replaceAll(`{${name}}`, String(value)),
        template);
}

function setProjectStatus(status, labelKey, name, revision = "") {
    elements.projectStatus.classList.remove("loading", "ready", "saved", "error");
    elements.projectStatus.classList.add(status);
    elements.projectStatusLabel.textContent = t(labelKey);
    elements.projectName.textContent = name;
    elements.projectRevision.textContent = revision;
}

function formatSavedAt(value) {
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return value || "";
    return new Intl.DateTimeFormat(locale === "ko" ? "ko-KR" : "en-US", {
        dateStyle: "medium",
        timeStyle: "short"
    }).format(date);
}

function safeFileName(value) {
    return value.toLowerCase().replace(/[^a-z0-9가-힣_-]+/g, "-");
}

function truncate(value, length) {
    const text = String(value || "");
    return text.length > length ? `${text.slice(0, length - 1)}…` : text;
}

function clear(node) {
    node.replaceChildren();
}

function el(name, className) {
    const node = document.createElement(name);
    if (className) node.className = className;
    return node;
}

function svg(name) {
    return document.createElementNS(SVG_NS, name);
}
