from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin
from accounts.models import User, WorkerProfile, BusinessProfile


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    list_display = ["email", "role", "is_verified", "is_active", "date_joined"]
    list_filter = ["role", "is_verified", "is_active"]
    search_fields = ["email"]
    ordering = ["-date_joined"]
    fieldsets = (
        (None, {"fields": ("email", "password")}),
        ("Info", {"fields": ("phone", "role", "is_verified")}),
        ("Permissions", {"fields": ("is_active", "is_staff", "is_superuser", "groups", "user_permissions")}),
    )
    add_fieldsets = (
        (None, {"fields": ("email", "password1", "password2", "role")}),
    )
    filter_horizontal = ("groups", "user_permissions")


@admin.register(WorkerProfile)
class WorkerProfileAdmin(admin.ModelAdmin):
    list_display = ["full_name", "user", "location_city", "is_available"]
    search_fields = ["full_name", "user__email"]


@admin.register(BusinessProfile)
class BusinessProfileAdmin(admin.ModelAdmin):
    list_display = ["legal_name", "user", "verification_status", "location_city"]
    list_filter = ["verification_status"]
    search_fields = ["legal_name", "user__email"]
