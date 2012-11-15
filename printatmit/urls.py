from django.conf.urls.defaults import patterns, include, url

# Uncomment the next two lines to enable the admin:
from django.contrib import admin
admin.autodiscover()

from django.conf import settings
from printatmit.printers import views

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'printatmit.views.home', name='home'),
    # url(r'^printatmit/', include('printatmit.foo.urls')),
    url(r'^query/$', views.query),
    url(r'^query_result/$', views.query_result),
    url(r'^printer_data/(\w+-?\w*)/$', views.printer_data),
    url(r'^update_results/$', views.update),
    url(r'^convert_url/$', views.get_pdf_from_url),    

    # Uncomment the admin/doc line below to enable admin documentation:
    # url(r'^admin/doc/', include('django.contrib.admindocs.urls')),

    # Uncomment the next line to enable the admin:
    url(r'^admin/', include(admin.site.urls)),
    url(r'^media/(?P<path>.*)$', 'django.views.static.serve', {'document_root': settings.MEDIA_ROOT}),
)

