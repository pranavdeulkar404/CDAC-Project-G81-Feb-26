using SprintFlow.AuditService.Models;

namespace SprintFlow.AuditService.Services;

public interface IAuditEventSink
{
    Task WriteAsync(AuditEventRequest auditEvent, CancellationToken cancellationToken);
}
