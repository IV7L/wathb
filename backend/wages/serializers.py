from rest_framework import serializers
from wages.models import WageObligation, PaymentDeclaration, PaymentEvidence, PaymentMethod


class WageObligationSerializer(serializers.ModelSerializer):
    worker_phone = serializers.CharField(source="assignment.worker.phone", read_only=True)
    worker_name = serializers.SerializerMethodField()
    job_title = serializers.CharField(source="assignment.job.title", read_only=True)
    amount_display = serializers.SerializerMethodField()
    declarations = serializers.SerializerMethodField()

    class Meta:
        model = WageObligation
        fields = [
            "id", "assignment", "worker_phone", "worker_name", "job_title",
            "amount", "currency", "amount_display", "due_date", "status", "created_at",
            "declarations",
        ]
        read_only_fields = fields

    def get_worker_name(self, obj):
        try:
            return obj.assignment.worker.worker_profile.full_name
        except Exception:
            return None

    def get_amount_display(self, obj):
        major = obj.amount / 100
        return f"{major:.0f} {obj.currency}"

    def get_declarations(self, obj):
        from wages.serializers import PaymentDeclarationSerializer
        return PaymentDeclarationSerializer(obj.declarations.all(), many=True).data


class PaymentDeclarationSerializer(serializers.ModelSerializer):
    declared_by_phone = serializers.CharField(source="declared_by.phone", read_only=True)

    class Meta:
        model = PaymentDeclaration
        fields = [
            "id", "wage_obligation", "declared_amount", "currency",
            "payment_date", "payment_method", "transaction_reference",
            "declared_by_phone", "declared_at", "status", "review_notes",
        ]
        read_only_fields = ["id", "declared_by_phone", "declared_at", "status", "review_notes"]


class PaymentDeclarationCreateSerializer(serializers.ModelSerializer):
    class Meta:
        model = PaymentDeclaration
        fields = ["declared_amount", "currency", "payment_date", "payment_method", "transaction_reference"]

    def validate_declared_amount(self, value):
        if value <= 0:
            raise serializers.ValidationError("Declared amount must be positive.")
        return value

    def validate_payment_method(self, value):
        allowed = [m.value for m in PaymentMethod]
        if value not in allowed:
            raise serializers.ValidationError(
                f"Invalid payment method. Choose from: {', '.join(allowed)}."
            )
        return value


class PaymentEvidenceSerializer(serializers.ModelSerializer):
    class Meta:
        model = PaymentEvidence
        fields = ["id", "original_filename", "content_type", "file_size", "uploaded_at"]
        read_only_fields = fields
