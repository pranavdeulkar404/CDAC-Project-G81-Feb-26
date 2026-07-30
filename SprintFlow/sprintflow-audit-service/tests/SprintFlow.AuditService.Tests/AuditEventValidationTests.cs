using System.ComponentModel.DataAnnotations;
using SprintFlow.AuditService.Models;

namespace SprintFlow.AuditService.Tests;

public sealed class AuditEventValidationTests
{
    [Fact]
    public void RejectsOversizedAttributeCollection()
    {
        var request = new AuditEventRequest
        {
            EventId = Guid.NewGuid(),
            OccurredAt = DateTimeOffset.UtcNow,
            EventType = AuditEventType.BugUpdated,
            Source = "sprintflow-api",
            EntityType = "BUG",
            EntityId = 30,
            ActorId = 1,
            ActorName = "Manager",
            Summary = "Updated bug.",
            Attributes = Enumerable.Range(1, 26)
                .ToDictionary(number => $"key{number}", number => number.ToString())
        };
        var results = new List<ValidationResult>();

        var valid = Validator.TryValidateObject(
            request,
            new ValidationContext(request),
            results,
            validateAllProperties: true);

        Assert.False(valid);
        Assert.Contains(results, result => result.MemberNames.Contains(nameof(AuditEventRequest.Attributes)));
    }
}
