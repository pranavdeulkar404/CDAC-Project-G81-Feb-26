# SprintFlow audit and telemetry service

This ASP.NET Core service receives best-effort project, task, and bug activity from
the Spring Boot API. It writes each accepted event to the live terminal and to a
structured rolling JSON file. It does not change SprintFlow data and is not a
legal/compliance audit ledger.

## Start

```powershell
Set-Location "C:\path\to\SprintFlow\sprintflow-audit-service"
dotnet run
```

The service listens locally on `http://localhost:8082`.

- Health: `http://localhost:8082/health`
- OpenAPI document: `http://localhost:8082/openapi/v1.json`
- Event receiver: `POST http://localhost:8082/api/audit-events`

Start this service before `sprintflow-api` when you want to capture every event.
The API still saves normal work if this service is stopped.

## Logs

Runtime files are created automatically in:

```text
sprintflow-audit-service\logs\sprintflow-audit-YYYYMMDD.json
```

Each line is a complete JSON event. Files roll every day and at 10 MB, and the
latest 14 files are retained. The generated `logs`, `bin`, and `obj` folders are
excluded from Git.

Watch today's log live:

```powershell
Get-Content ".\logs\sprintflow-audit-$(Get-Date -Format yyyyMMdd).json" -Wait
```

## Direct smoke test

With the service running:

```powershell
$event = @{
    eventId = [guid]::NewGuid()
    occurredAt = [DateTimeOffset]::UtcNow.ToString("o")
    eventType = "TASK_CREATED"
    source = "manual-smoke-test"
    entityType = "TASK"
    entityId = 20
    actorId = 1
    actorName = "Local tester"
    summary = 'Created task "Audit smoke test".'
    attributes = @{
        projectId = "10"
        status = "TODO"
    }
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8082/api/audit-events" `
    -ContentType "application/json" `
    -Body $event
```

The response is HTTP `202 Accepted`. The event then appears in the service
terminal and the current JSON log file.

## Test

```powershell
dotnet test ".\tests\SprintFlow.AuditService.Tests\SprintFlow.AuditService.Tests.csproj"
```
