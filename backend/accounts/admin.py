import secrets

from django.contrib import admin, messages
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from accounts.models import User, WorkerProfile, BusinessProfile


@admin.action(description="إعادة تعيين كلمة المرور (تعرض الكلمة الجديدة مرة واحدة)")
def reset_password(modeladmin, request, queryset):
    parts = []
    for user in queryset:
        new_pass = secrets.token_urlsafe(10)
        user.set_password(new_pass)
        user.save(update_fields=["password"])
        parts.append(f"{user.phone}: {new_pass}")
    modeladmin.message_user(
        request,
        "كلمات المرور الجديدة — احفظها الآن: " + " | ".join(parts),
        level=messages.WARNING,
    )


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    actions = [reset_password]
    list_display = ["phone", "email", "role", "is_verified", "is_active", "date_joined"]
    list_filter = ["role", "is_verified", "is_active"]
    search_fields = ["phone", "email"]
    ordering = ["-date_joined"]
    fieldsets = (
        (None, {"fields": ("phone", "password")}),
        ("Info", {"fields": ("email", "role", "is_verified")}),
        ("Permissions", {"fields": ("is_active", "is_staff", "is_superuser", "groups", "user_permissions")}),
    )
    add_fieldsets = (
        (None, {"fields": ("phone", "password1", "password2", "role")}),
    )
    filter_horizontal = ("groups", "user_permissions")


@admin.register(WorkerProfile)
class WorkerProfileAdmin(admin.ModelAdmin):
    list_display = ["full_name", "user", "location_city", "is_available"]
    search_fields = ["full_name", "user__phone"]


@admin.register(BusinessProfile)
class BusinessProfileAdmin(admin.ModelAdmin):
    list_display = ["legal_name", "user", "verification_status", "location_city"]
    list_filter = ["verification_status"]
    search_fields = ["legal_name", "user__phone"]
