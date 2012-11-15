import os
import sys

os.environ['DJANGO_SETTINGS_MODULE'] = 'printatmit.settings'

import django.core.handlers.wsgi
application = django.core.handlers.wsgi.WSGIHandler()
sys.path.append('/home/printapi')
sys.path.append('/home/printapi/printatmit')
