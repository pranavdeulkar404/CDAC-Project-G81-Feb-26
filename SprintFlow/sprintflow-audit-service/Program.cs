using System.ComponentModel.DataAnnotations;
using SprintFlow.AuditService.Models;
using SprintFlow.AuditService.Services;
using Serilog;

var builder = WebApplication.CreateBuilder(args);

builder.Host.UseSerilog((context, services, loggerConfiguration) =>
{
    loggerConfiguration
        .ReadFrom.Configuration(context.Configuration)
        .ReadFrom.Services(services);
});

builder.Services.AddOpenApi();
builder.Services.AddSingleton<IAuditEventSink, StructuredLogAuditEventSink>();

var app = builder.Build();

app.MapOpenApi();

app.MapGet("/health", () => Results.Ok(new
{
    status = "UP",
    service = "sprintflow-audit-service",
    timestamp = DateTimeOffset.UtcNow
})).WithName("Health");

app.MapPost("/api/audit-events", async (
    AuditEventRequest request,
    IAuditEventSink sink,
    CancellationToken cancellationToken) =>
{
    var validationResults = new List<ValidationResult>();
    var validationContext = new ValidationContext(request);

    if (!Validator.TryValidateObject(request, validationContext, validationResults, validateAllProperties: true))
    {
        var errors = validationResults
            .SelectMany(result => result.MemberNames.DefaultIfEmpty("request")
                .Select(member => new { member, message = result.ErrorMessage ?? "Invalid value" }))
            .GroupBy(error => error.member)
            .ToDictionary(
                group => group.Key,
                group => group.Select(error => error.message).Distinct().ToArray());

        return Results.ValidationProblem(errors);
    }

    await sink.WriteAsync(request, cancellationToken);
    return Results.Accepted(value: new
    {
        request.EventId,
        status = "accepted"
    });
})
.WithName("RecordAuditEvent")
.Produces(StatusCodes.Status202Accepted)
.ProducesValidationProblem();

app.Run();

public partial class Program;
