using System.ComponentModel.DataAnnotations;

namespace SprintFlow.AuditService.Models;

public sealed class AuditEventRequest : IValidatableObject
{
    public Guid EventId { get; init; }

    public DateTimeOffset OccurredAt { get; init; }

    public AuditEventType EventType { get; init; }

    [Required, StringLength(80)]
    public string Source { get; init; } = string.Empty;

    [Required, StringLength(40)]
    public string EntityType { get; init; } = string.Empty;

    [Range(1, long.MaxValue)]
    public long EntityId { get; init; }

    [Range(1, long.MaxValue)]
    public long ActorId { get; init; }

    [Required, StringLength(120)]
    public string ActorName { get; init; } = string.Empty;

    [Required, StringLength(500)]
    public string Summary { get; init; } = string.Empty;

    public IReadOnlyDictionary<string, string> Attributes { get; init; }
        = new Dictionary<string, string>();

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        if (EventId == Guid.Empty)
        {
            yield return new ValidationResult(
                "EventId must not be empty.",
                [nameof(EventId)]);
        }

        if (OccurredAt == default)
        {
            yield return new ValidationResult(
                "OccurredAt must be provided.",
                [nameof(OccurredAt)]);
        }

        if (Attributes.Count > 25)
        {
            yield return new ValidationResult(
                "Attributes cannot contain more than 25 entries.",
                [nameof(Attributes)]);
        }

        if (Attributes.Any(attribute =>
                string.IsNullOrWhiteSpace(attribute.Key)
                || attribute.Key.Length > 80
                || attribute.Value?.Length > 500))
        {
            yield return new ValidationResult(
                "Attribute keys must be 1-80 characters and values at most 500 characters.",
                [nameof(Attributes)]);
        }
    }
}
