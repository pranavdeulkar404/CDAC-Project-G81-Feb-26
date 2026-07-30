using SprintFlow.AuditService.Models;

namespace SprintFlow.AuditService.Services;

public sealed class StructuredLogAuditEventSink(
    ILogger<StructuredLogAuditEventSink> logger) : IAuditEventSink
{
    public Task WriteAsync(AuditEventRequest auditEvent, CancellationToken cancellationToken)
    {
        cancellationToken.ThrowIfCancellationRequested();
        logger.LogInformation("Audit event received: {@AuditEvent}", auditEvent);
        return Task.CompletedTask;
    }
}
