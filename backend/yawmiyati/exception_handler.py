from rest_framework.views import exception_handler
from rest_framework.response import Response
from rest_framework import status


def custom_exception_handler(exc, context):
    response = exception_handler(exc, context)

    if response is not None:
        error_code = "error"
        detail = str(exc)
        fields = None

        data = response.data
        if isinstance(data, dict):
            if "detail" in data:
                detail = str(data["detail"])
            else:
                error_code = "validation_error"
                detail = "One or more fields are invalid."
                fields = data

        response.data = {
            "error": error_code,
            "detail": detail,
        }
        if fields:
            response.data["fields"] = fields

    return response
