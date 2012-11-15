# Create your views here.
from django.shortcuts import render_to_response
from django.http import HttpResponseRedirect, HttpResponse, HttpResponseBadRequest
from django.core import serializers

from printers.models import Printer
import util
import json
import re
import subprocess
import urllib

def query(request):
    return render_to_response("query.html", {})

def printer_data(request, printer):
    if request.method=='GET' and 'printer_query' in request.GET:
        query = request.GET['printer_query']
        data = serializers.serialize("json", Printer.objects.filter(pk=query))
    else:
        if printer == "all":
            data = serializers.serialize("json", Printer.objects.all())
        else:
            data = serializers.serialize("json", Printer.objects.filter(pk=printer))
    return render_to_response("printer_data.html", {"data":data})

def query_result(request):
    output=[]
    
    if request.method=='GET':
        if 'printer_query' in request.GET:
            query = request.GET['printer_query']
            data = [p.get_dict('name') for p in Printer.objects.filter(pk=query)]
            output = json.dumps(data)
            
        elif 'sort' in request.GET:
            #sort by printer name
            if request.GET['sort'] == 'name':
                list = [p.get_dict('name') for p in Printer.objects.order_by('pk')]
                output = json.dumps(list)

            #sort by building name
            elif request.GET['sort'] == 'building':
                list = [p.get_dict('building') for p in Printer.objects.order_by('building_name')]
                output = json.dumps(_alphanumeric_sort(list))

            #sort by distance to gps location
            elif request.GET['sort'] == 'distance': 
                if 'latitude' and 'longitude' in request.GET:
                    lat = float(request.GET['latitude'])
                    long = float(request.GET['longitude'])
                    list = sorted([p.get_dict('distance', lat, long) for p in Printer.objects.all()], key = lambda dict: dict['distance'])
                    output = json.dumps(list)
    return render_to_response("query_results.html", {"output":output})


def _alphanumeric_sort(list):
    building_list = [[], [], []]
    building_list[0] = []
    building_list[1] = []
    building_list[2] = []
    section = 0
    for printer in list:
        if printer['building_name'].find("Building") == -1  and section == 1:
            section = 2
        elif printer['building_name'].find("Building") != -1 and section == 0:
            section = 1
        building_list[section].append(printer)
    #tmp1 = sorted(building_list[1], key = _getcomparator) 
    tmp1 = []
    tmp2 = []
    building = []
    for printer in building_list[1]:
        key = printer['building_name'].split()[1]
        match1 = re.search(r'[A-Z]\d+', key)
        match2 = re.search(r'\d+', key)
        if match1:
           printer['key'] = match1.group()
           tmp1.append(printer)
        elif match2:  
           printer['key'] = int(match2.group())
           tmp2.append(printer)
    building = sorted(tmp2, key = lambda p:p['key'])
    building.extend(sorted(tmp1, key=lambda p:p['key']))
    output = building_list[0]
    output.extend(building)
    output.extend(building_list[2])
    return output
             

def update(request):
    output = []
    for printer in util.PRINTERS:
        #if not in database, create new Printer object
        #else update, go through status and error message fields to look for changes 
        filter = Printer.objects.filter(pk=printer)
        printer_data = util.get_printer_data(printer)
        if len(filter) == 0: #creating new object
            #in case printer loses connection
            if len(printer_data) != 0:
                new_obj = Printer(name=printer_data['name'], 
                                  location=printer_data['location'],
                                  building_name=printer_data['building_name'],
                                  atResidence=printer_data['atResidence'],
                                  latitude=printer_data['latitude'],
                                  longitude=printer_data['longitude'],
                                  error_msg=printer_data['error_msg'],
                                  status=printer_data['status'])
               	new_obj.save()
                output.append("Creating printer row for: " + printer)
                output.append(serializers.serialize("json", Printer.objects.filter(pk=printer)))
        else: #updating object
            obj = filter[0]
            obj.building_name = printer_data['building_name'] 
            obj.error_msg = printer_data['error_msg']
            obj.status = printer_data['status']
            obj.save()
            output.append("Updating printer row for: " + printer)
            output.append(serializers.serialize("json", Printer.objects.filter(pk=printer)))
    return render_to_response("update_results.html", {'output':output})

def get_pdf_from_url(request):
    """
    Runs commandline wkhtmltopdf.
    Returns http 400 on error.
    """
    response = HttpResponse()
    if request.method == 'GET':
        if 'url' in request.GET:
            url = urllib.unquote(request.GET['url'])
            command = "/home/printapi/printatmit/printers/wkhtmltopdf-amd64 -s Letter " + url + " -"
            process = subprocess.Popen([command], bufsize=4096, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True)
            pdf, error = process.communicate()
            #if stdout stream has length 0, then there was an error, possibly not robust, should test some more later
            if len(pdf) == 0:
                response = HttpResponseBadRequest(error)
            else:
                response = HttpResponse(mimetype="application/pdf")
                response['Content-Disposition'] = "filename=printAtMIT.pdf"            
                response.write(pdf)    

    else:
        response = HttpResponseBadRequest("not get request")
    return response
