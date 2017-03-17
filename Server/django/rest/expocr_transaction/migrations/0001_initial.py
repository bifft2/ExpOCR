# -*- coding: utf-8 -*-
# Generated by Django 1.10.6 on 2017-03-17 12:34
from __future__ import unicode_literals

from django.db import migrations, models
import django.db.models.manager


class Migration(migrations.Migration):

    initial = True

    dependencies = [
    ]

    operations = [
        migrations.CreateModel(
            name='Transaction',
            fields=[
                ('T_Id', models.AutoField(primary_key=True, serialize=False, unique=True)),
                ('Sender_Id', models.IntegerField()),
                ('Receiver_Id', models.IntegerField()),
                ('Category', models.CharField(max_length=255)),
                ('Memo', models.CharField(max_length=255)),
                ('Amount', models.DecimalField(decimal_places=2, max_digits=17)),
                ('Date', models.DateTimeField()),
            ],
            options={
                'ordering': ['T_Id'],
                'db_table': 'transactions',
            },
            managers=[
                ('manager', django.db.models.manager.Manager()),
            ],
        ),
    ]
