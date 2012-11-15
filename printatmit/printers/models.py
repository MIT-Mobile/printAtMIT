from django.db import models
import util

# Create your models here.
class Printer(models.Model):
    name = models.CharField(max_length=50, primary_key=True)
    #building/room number
    location = models.CharField(max_length=30)
    #i.e dorm names
    building_name = models.CharField(max_length=30, null=True, blank=True)
    atResidence = models.BooleanField()
    latitude = models.IntegerField()
    longitude = models.IntegerField()
    #error message if error, else null or blank
    error_msg = models.CharField(max_length=30, null=True, blank=True)
    #0 = available, 1 = busy, 2 = error
    status = models.IntegerField(choices=((0, u'Available'), (1, u'Busy'), (2, u'Error'))) 

    def get_dict(self, sort_type, lat=None, long=None):
        dict = {}
        dict['name'] = self.name
        dict['location'] = self.location
        dict['building_name'] = self.building_name
        dict['atResidence'] = self.atResidence
        dict['latitude'] = self.latitude
        dict['longitude'] = self.longitude
        dict['error_msg'] = self.error_msg
        dict['status'] = self.status
        if sort_type == 'name':
            dict['section_header'] = dict['name'][0]
            dict['distance'] = 0.0
        elif sort_type == 'building':
            dict['section_header'] = dict['building_name']
            dict['distance'] = 0.0
        elif sort_type == 'distance':
            if lat == None or long == None:
                dict['section_header'] = ""
            else:
                dist = util.get_distance(lat, long, self.latitude / 1000000.0, self.longitude / 1000000.0)
                dict['distance'] = dist
                if dist < 0.1:
                    dict['section_header'] = "< 0.1 miles"
                elif dist >= 0.1 and dist < 0.3:
                    dict['section_header'] = "0.1 - 0.3 miles"
                elif dist >= 0.3 and dist < 0.5:
                    dict['section_header'] = "0.3 - 0.5 miles"
                elif dist >=0.5 and dist < 1.0:
                    dict['section_header'] = "0.5 - 1.0 miles"
                else:
                    dict['section_header'] = "> 1.0 miles"
        else:
            dict['section_header'] = ""
        return dict
            
