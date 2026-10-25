package dev.vlad.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

// `contract/openapi.yaml` şemalarının Kotlin karşılığı (S7a: okuma yönü + istek gövdeleri). Zaman damgaları ISO-8601 metin.

/** Bilinmeyen alanlar yok sayılır (sunucu alan eklerse eski istemci çökmez); varsayılanlar kodlanmaz. */
val VladJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

@Serializable
enum class ErrorCode {
    @SerialName("unauthorized")
    UNAUTHORIZED,

    @SerialName("expired")
    EXPIRED,

    @SerialName("head_moved")
    HEAD_MOVED,

    @SerialName("ci_not_green")
    CI_NOT_GREEN,

    @SerialName("not_mergeable")
    NOT_MERGEABLE,

    @SerialName("not_delegable")
    NOT_DELEGABLE,

    @SerialName("grant_invalid")
    GRANT_INVALID,

    @SerialName("device_revoked")
    DEVICE_REVOKED,

    @SerialName("bad_request")
    BAD_REQUEST,

    @SerialName("rate_limited")
    RATE_LIMITED,

    @SerialName("quota_hold")
    QUOTA_HOLD,

    @SerialName("active_elsewhere")
    ACTIVE_ELSEWHERE,
}

@Serializable
data class ErrorBody(
    val code: ErrorCode,
    @SerialName("message_tr") val messageTr: String,
    val details: JsonObject? = null,
)

@Serializable
data class ErrorResponse(val error: ErrorBody)

@Serializable
data class OkResponse(val ok: Boolean)

@Serializable
data class Health(val ok: Boolean, val version: String)

@Serializable
data class PairRequest(
    val code: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("session_pub") val sessionPub: String,
    @SerialName("approval_pub") val approvalPub: String,
    @SerialName("fcm_token") val fcmToken: String? = null,
)

@Serializable
data class PairResponse(
    @SerialName("device_id") val deviceId: String,
    @SerialName("server_time") val serverTime: Long,
)

@Serializable
enum class TaskState {
    @SerialName("queued")
    QUEUED,

    @SerialName("running")
    RUNNING,

    @SerialName("blocked")
    BLOCKED,

    @SerialName("stalled")
    STALLED,

    @SerialName("awaiting_approval")
    AWAITING_APPROVAL,

    @SerialName("done")
    DONE,

    @SerialName("failed")
    FAILED,

    @SerialName("cancelled")
    CANCELLED,
}

@Serializable
data class Progress(val pct: Int? = null, val label: String, val at: String)

@Serializable
data class PullRequestRef(val number: Int, val repo: String, val base: String, val url: String? = null)

@Serializable
data class Worker(
    @SerialName("task_id") val taskId: String,
    val project: String,
    val title: String,
    val phase: String? = null,
    val state: TaskState,
    val host: String? = null,
    val progress: Progress? = null,
    val updated: String,
)

@Serializable
data class Quota(val limited: Boolean, val until: String? = null)

@Serializable
data class ActiveVlad(val machine: String, val since: String, val heartbeat: String, val mine: Boolean)

@Serializable
enum class CardKind {
    @SerialName("ci_red")
    CI_RED,

    @SerialName("conflict")
    CONFLICT,

    @SerialName("vlad_question")
    VLAD_QUESTION,

    @SerialName("action_request")
    ACTION_REQUEST,

    @SerialName("kb_conflict")
    KB_CONFLICT,

    @SerialName("gate")
    GATE,
}

@Serializable
enum class CardState {
    @SerialName("open")
    OPEN,

    @SerialName("resolved")
    RESOLVED,

    @SerialName("dismissed")
    DISMISSED,
}

@Serializable
data class CardOption(val id: String, val label: String)

@Serializable
data class CardAnswer(
    val option: String? = null,
    val text: String? = null,
    @SerialName("by_device") val byDevice: String,
    val at: String,
)

@Serializable
enum class CardActionKind {
    @SerialName("merge_phase")
    MERGE_PHASE,

    @SerialName("merge_final")
    MERGE_FINAL,

    @SerialName("push")
    PUSH,

    @SerialName("tag")
    TAG,
}

@Serializable
data class CardAction(
    val kind: CardActionKind,
    val repo: String,
    val pr: Int? = null,
    val base: String? = null,
    val ref: String? = null,
    val sha: String,
)

@Serializable
enum class CiState {
    @SerialName("green")
    GREEN,

    @SerialName("red")
    RED,

    @SerialName("pending")
    PENDING,

    @SerialName("none")
    NONE,
}

@Serializable
enum class ReviewVerdict {
    @SerialName("clear")
    CLEAR,

    @SerialName("findings")
    FINDINGS,

    @SerialName("unknown")
    UNKNOWN,
}

@Serializable
data class GateReview(val source: String, val verdict: ReviewVerdict, val summary: String? = null)

@Serializable
data class GateInfo(
    @SerialName("task_id") val taskId: String,
    val phase: String? = null,
    val pr: PullRequestRef? = null,
    @SerialName("head_sha") val headSha: String,
    val ci: CiState,
    val mergeable: Boolean? = null,
    @SerialName("report_md") val reportMd: String,
    @SerialName("merge_kind") val mergeKind: CardActionKind? = null,
    val reviews: List<GateReview>,
)

@Serializable
data class Card(
    val id: String,
    val kind: CardKind,
    val project: String,
    val task: String? = null,
    val state: CardState,
    val title: String,
    val options: List<CardOption>,
    val answer: CardAnswer? = null,
    val action: CardAction? = null,
    val gate: GateInfo? = null,
    val created: String,
    val updated: String,
)

@Serializable
data class Home(
    val seq: Long,
    val asks: List<Card>,
    val workers: List<Worker>,
    val quota: Quota,
    @SerialName("active_vlad") val activeVlad: ActiveVlad,
    val warnings: List<String>,
)

@Serializable
data class CardAnswerRequest(val option: String? = null, val text: String? = null)

@Serializable
data class RequestChangesRequest(val text: String)

@Serializable
data class Task(
    val id: String,
    val project: String,
    val title: String,
    val kind: String,
    val state: TaskState,
    val autonomy: String,
    val role: String? = null,
    val parent: String? = null,
    @SerialName("depends_on") val dependsOn: List<String> = emptyList(),
    val phase: String? = null,
    val host: String? = null,
    val progress: Progress? = null,
    val pr: PullRequestRef? = null,
    val error: String? = null,
    @SerialName("report_md") val reportMd: String? = null,
    val created: String,
    val updated: String,
)

@Serializable
data class PipelineNode(
    @SerialName("task_id") val taskId: String,
    val title: String,
    val state: TaskState,
    val phase: String? = null,
    val role: String? = null,
    @SerialName("depends_on") val dependsOn: List<String>,
    val progress: Progress? = null,
)

@Serializable
data class Pipeline(
    @SerialName("task_id") val taskId: String,
    val project: String,
    val title: String,
    val state: TaskState,
    val phases: List<PipelineNode>,
)

@Serializable
data class PipelineList(val pipelines: List<Pipeline>, val warnings: List<String>)

@Serializable
data class ProjectSummary(
    val slug: String,
    val name: String,
    val exposure: String,
    val repo: String? = null,
    @SerialName("open_tasks") val openTasks: Int,
    @SerialName("last_activity") val lastActivity: String? = null,
)

@Serializable
data class Project(
    val slug: String,
    val name: String,
    val exposure: String,
    val repo: String? = null,
    @SerialName("open_tasks") val openTasks: Int,
    @SerialName("last_activity") val lastActivity: String? = null,
    @SerialName("card_md") val cardMd: String,
)

@Serializable
data class ProjectList(val projects: List<ProjectSummary>, val warnings: List<String>)

@Serializable
data class Finding(
    val id: String,
    val project: String,
    val severity: String,
    val status: String,
    val title: String,
    val detail: String? = null,
)

@Serializable
data class FindingList(val findings: List<Finding>, val warnings: List<String>)

@Serializable
data class VaultHit(val path: String, val score: Double, val snippet: String, val heading: String? = null)

@Serializable
data class VaultSearchResponse(val hits: List<VaultHit>)

@Serializable
data class VaultFile(val path: String, val content: String, val headings: List<String>)

@Serializable
data class ChatRequest(
    @SerialName("client_msg_id") val clientMsgId: String,
    val text: String,
)

@Serializable
data class ChatAccepted(
    @SerialName("chat_id") val chatId: String,
)

@Serializable
data class ChatMessage(
    @SerialName("chat_id") val chatId: String,
    val role: String,
    val text: String,
    val at: String,
)

@Serializable
data class ChatHistory(
    val messages: List<ChatMessage>,
    @SerialName("has_more") val hasMore: Boolean,
)

@Serializable
enum class ActionKind {
    @SerialName("merge_phase")
    MERGE_PHASE,

    @SerialName("merge_final")
    MERGE_FINAL,

    @SerialName("tag_release")
    TAG_RELEASE,
}

@Serializable
data class PrepareRequest(
    val kind: ActionKind,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("card_id") val cardId: String? = null,
    val params: Map<String, String>? = null,
)

/** Alanlar challenge bayt dizisindeki sırayla; kart yalnız buradan çizilir. */
@Serializable
data class ChallengeFields(
    @SerialName("action_id") val actionId: String,
    val kind: String,
    val repo: String,
    val pr: String,
    val base: String,
    @SerialName("head_sha") val headSha: String,
    val task: String,
    val nonce: String,
    val exp: String,
) {
    fun asMap(): Map<String, String> =
        linkedMapOf(
            "action_id" to actionId, "kind" to kind, "repo" to repo, "pr" to pr, "base" to base,
            "head_sha" to headSha, "task" to task, "nonce" to nonce, "exp" to exp,
        )
}

@Serializable
data class Challenge(
    @SerialName("action_id") val actionId: String,
    @SerialName("bytes_b64") val bytesB64: String,
    val fields: ChallengeFields,
    val exp: Long,
)

@Serializable
data class ApproveRequest(val signature: String)

@Serializable
data class ApproveResult(
    val kind: String,
    val repo: String,
    val pr: Int,
    @SerialName("head_sha") val headSha: String,
    @SerialName("merge_sha") val mergeSha: String? = null,
)

@Serializable
data class ApproveResponse(val status: String, val result: ApproveResult)

@Serializable
data class GrantScope(val type: String, val ref: String)

@Serializable
data class GrantCreateRequest(
    val id: String,
    val scope: GrantScope,
    val action: String,
    val conditions: List<String>,
    val created: String,
    val expires: String,
    @SerialName("max_uses") val maxUses: Int,
    val signature: String,
    @SerialName("payload_version") val payloadVersion: String = "v1",
)

@Serializable
data class GrantCreated(val id: String)

@Serializable
data class Grant(
    val id: String,
    val scope: GrantScope,
    val action: String,
    val conditions: List<String>,
    val created: String,
    val expires: String,
    @SerialName("max_uses") val maxUses: Int,
    val uses: Int,
    val status: String,
    @SerialName("device_id") val deviceId: String,
)

@Serializable
data class GrantList(val grants: List<Grant>, val warnings: List<String>)

@Serializable
data class Device(
    @SerialName("device_id") val deviceId: String,
    @SerialName("device_name") val deviceName: String,
    val created: String,
    @SerialName("last_seen") val lastSeen: String? = null,
    val revoked: Boolean,
    @SerialName("has_fcm") val hasFcm: Boolean,
    val me: Boolean,
)

@Serializable
data class DeviceList(val devices: List<Device>)

@Serializable
data class FcmRequest(
    @SerialName("fcm_token") val fcmToken: String,
)

@Serializable
data class RevokeDeviceRequest(
    @SerialName("ts_ms") val tsMs: Long,
    val signature: String,
)

@Serializable
enum class EventType(val wire: String) {
    @SerialName("card.opened")
    CARD_OPENED("card.opened"),

    @SerialName("task.state")
    TASK_STATE("task.state"),

    @SerialName("worker.progress")
    WORKER_PROGRESS("worker.progress"),

    @SerialName("chat.delta")
    CHAT_DELTA("chat.delta"),

    @SerialName("chat.done")
    CHAT_DONE("chat.done"),

    @SerialName("action.executed")
    ACTION_EXECUTED("action.executed"),

    @SerialName("grant.used")
    GRANT_USED("grant.used"),

    @SerialName("quota.changed")
    QUOTA_CHANGED("quota.changed"),
}

@Serializable
data class Event(val seq: Long, val type: EventType, val data: JsonObject)

@Serializable
data class EventList(
    val events: List<Event>,
    @SerialName("last_seq") val lastSeq: Long,
)
