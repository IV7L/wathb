from django.db import migrations, models


class Migration(migrations.Migration):
    dependencies = [
        ("jobs", "0002_job_category"),
    ]

    operations = [
        migrations.AddField(
            model_name="joblisting",
            name="thumbnail_url",
            field=models.URLField(blank=True, default="", max_length=500),
        ),
    ]
