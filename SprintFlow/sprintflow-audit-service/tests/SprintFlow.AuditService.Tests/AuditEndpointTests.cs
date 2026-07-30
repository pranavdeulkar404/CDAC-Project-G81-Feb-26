using System.Net;
using System.Net.Http.Json;
using System.Collections.Concurrent;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.AspNetCore.TestHost;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using SprintFlow.AuditService.Models;
using SprintFlow.AuditService.Services;

namespace SprintFlow.AuditService.Tests;

public sealed class AuditEndpointTests : IClassFixture<AuditEndpointTests.AuditServiceFactory>
{
    private readonly AuditServiceFactory factory;

    public AuditEndpointTests(AuditServiceFactory factory)
    {
        this.factory = factory;
    }

    [Fact]
    public async Task ValidEventIsAcceptedAndForwardedToSink()
    {
        var client = factory.CreateClient();
        var request = ValidRequest();

        var response = await client.PostAsJsonAsync("/api/audit-events", request);

        Assert.Equal(HttpStatusCode.Accepted, response.StatusCode);
        Assert.Contains(factory.Sink.Events, auditEvent => auditEvent.EventId == request.EventId);
    }

    [Fact]
    public async Task InvalidEventReturnsValidationProblem()
    {
        var client = factory.CreateClient();
        var eventCountBeforeRequest = factory.Sink.Events.Count;
        var request = ValidRequest() with
        {
            EventId = Guid.Empty,
            EntityId = 0,
            Summary = string.Empty
        };

        var response = await client.PostAsJsonAsync("/api/audit-events", request);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Equal(eventCountBeforeRequest, factory.Sink.Events.Count);
    }

    private static TestAuditEvent ValidRequest() => new()
    {
        EventId = Guid.NewGuid(),
        OccurredAt = DateTimeOffset.UtcNow,
        EventType = AuditEventType.TaskCreated,
        Source = "sprintflow-api",
        EntityType = "TASK",
        EntityId = 20,
        ActorId = 1,
        ActorName = "Manager",
        Summary = "Created task \"Build screen\".",
        Attributes = new Dictionary<string, string>
        {
            ["projectId"] = "10",
            ["status"] = "TODO"
        }
    };

    public sealed class AuditServiceFactory : WebApplicationFactory<Program>
    {
        public CapturingAuditEventSink Sink { get; } = new();

        protected override void ConfigureWebHost(IWebHostBuilder builder)
        {
            builder.UseEnvironment("Testing");
            builder.ConfigureTestServices(services =>
            {
                services.RemoveAll<IAuditEventSink>();
                services.AddSingleton<IAuditEventSink>(Sink);
            });
        }
    }

    public sealed class CapturingAuditEventSink : IAuditEventSink
    {
        public ConcurrentQueue<AuditEventRequest> Events { get; } = new();

        public Task WriteAsync(AuditEventRequest auditEvent, CancellationToken cancellationToken)
        {
            Events.Enqueue(auditEvent);
            return Task.CompletedTask;
        }
    }

    private sealed record TestAuditEvent
    {
        public Guid EventId { get; init; }
        public DateTimeOffset OccurredAt { get; init; }
        public AuditEventType EventType { get; init; }
        public string Source { get; init; } = string.Empty;
        public string EntityType { get; init; } = string.Empty;
        public long EntityId { get; init; }
        public long ActorId { get; init; }
        public string ActorName { get; init; } = string.Empty;
        public string Summary { get; init; } = string.Empty;
        public IReadOnlyDictionary<string, string> Attributes { get; init; }
            = new Dictionary<string, string>();
    }
}
