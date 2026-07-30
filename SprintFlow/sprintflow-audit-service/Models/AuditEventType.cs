using System.Text.Json.Serialization;

namespace SprintFlow.AuditService.Models;

[JsonConverter(typeof(JsonStringEnumConverter))]
public enum AuditEventType
{
    [JsonStringEnumMemberName("PROJECT_CREATED")]
    ProjectCreated,
    [JsonStringEnumMemberName("PROJECT_UPDATED")]
    ProjectUpdated,
    [JsonStringEnumMemberName("PROJECT_DELETED")]
    ProjectDeleted,
    [JsonStringEnumMemberName("TASK_CREATED")]
    TaskCreated,
    [JsonStringEnumMemberName("TASK_UPDATED")]
    TaskUpdated,
    [JsonStringEnumMemberName("TASK_STATUS_CHANGED")]
    TaskStatusChanged,
    [JsonStringEnumMemberName("TASK_DELETED")]
    TaskDeleted,
    [JsonStringEnumMemberName("BUG_CREATED")]
    BugCreated,
    [JsonStringEnumMemberName("BUG_UPDATED")]
    BugUpdated,
    [JsonStringEnumMemberName("BUG_STATUS_CHANGED")]
    BugStatusChanged,
    [JsonStringEnumMemberName("BUG_DELETED")]
    BugDeleted
}
