from django.core.management.base import BaseCommand
from django.core import serializers
from printers import views
import time

class Command(BaseCommand):

  def handle(self, *args, **options):
    start = time.time()
    print views.update()
    print "time: " + str(time.time() - start)
