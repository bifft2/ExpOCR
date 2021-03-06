from __future__ import print_function
from __future__ import unicode_literals

from django.db import models
from django.db.models import Q, Sum


# Create your models here.

class Transaction(models.Model):
    T_Id = models.AutoField(primary_key=True, null=False, unique=True)
    Sender_Id = models.IntegerField(null=False)
    Receiver_Id = models.IntegerField(null=False)
    Category = models.CharField(max_length=255, null=False)
    Memo = models.CharField(max_length=255, null=False)
    Amount = models.DecimalField(max_digits=17, decimal_places=2, null=False)
    Date = models.DateTimeField(null=False)

    manager = models.Manager()

    class Meta:
        db_table = 'TRANSACTIONS'
        ordering = ['T_Id']

    @staticmethod
    def create_transaction(sender_id, receiver_id, category, memo, amount, date):
        result = Transaction.manager.create(Sender_Id=sender_id, Receiver_Id=receiver_id,
                                            Category=category, Memo=memo, Amount=amount, Date=date)
        return result

    @staticmethod
    def get_transaction_by_sender_id(sender_id):
        query = Q(Sender_Id=sender_id)
        result = Transaction.manager.filter(query).order_by('-Date') \
            .values('Sender_Id', 'Receiver_Id', 'Amount', 'Date', 'Category')
        return result

    @staticmethod
    def get_transaction_by_receiver_id(receiver_id):
        query = Q(Receiver_Id=receiver_id)
        result = Transaction.manager.filter(query).order_by('-Date') \
            .values('Sender_Id', 'Receiver_Id', 'Amount', 'Date', 'Category')
        return result

    @staticmethod
    def get_transaction_by_id(u_id):
        query = Q(Sender_Id=u_id) | Q(Receiver_Id=u_id)
        result = Transaction.manager.filter(query).order_by('-Date') \
            .values('Sender_Id', 'Receiver_Id', 'Amount', 'Date', 'Category')
        return result

    @staticmethod
    def get_all_friends_sender(user_id):
        # TODO friends tab
        query = Q(Sender_Id=user_id)
        result = Transaction.manager.filter(query).order_by('Receiver_Id'). \
            values('Receiver_Id').annotate(Sum('Amount'))
        return result

    @staticmethod
    def get_all_friends_receiver(user_id):
        # TODO friends tab
        query = Q(Receiver_Id=user_id)
        result = Transaction.manager.filter(query).order_by('Sender_Id'). \
            values('Sender_Id').annotate(Sum('Amount'))
        return result

    @staticmethod
    def get_transaction_between(sender_id, receiver_id):
        query = (
        (Q(Sender_Id=sender_id) & Q(Receiver_Id=receiver_id)) | (Q(Sender_Id=receiver_id) & Q(Receiver_Id=sender_id)))
        result = Transaction.manager.filter(query).order_by('-Date').values('Sender_Id', 'Receiver_Id', 'T_Id',
                                                                            'Category', 'Memo', 'Amount', 'Date')
        return result

    @staticmethod
    def get_transaction_by_t_id(t_id):
        query = Q(T_Id=t_id)
        result = Transaction.manager.filter(query).order_by('-Date').values('Sender_Id', 'Receiver_Id', 'Category',
                                                                            'Memo', 'Amount', 'Date')
        return result

    @staticmethod
    def delete_transaction_between(sender_id, receiver_id):
        query = Q(Sender_Id=sender_id) & Q(Receiver_Id=receiver_id)
        result = Transaction.manager.filter(query).delete()
        return result

    @staticmethod
    def delete_transaction_by_id(tid):
        query = Q(T_Id=tid)
        result = Transaction.manager.filter(query).delete()
        return result
