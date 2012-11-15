import subprocess
import re
import printer_list
import math

READY_STATUS = 0
BUSY_STATUS = 1
ERROR_STATUS = 2
READY_MESSAGES = set(["sleep mode on", "checking printer", "warming up", "ready"])
BUSY_MESSAGES = set(["processing job"])
PRINTERS = printer_list.PRINTERS
def get_printer_data(printer):
    """
    Param
        string printer: printername
    Return 
        output: dict containing printer data
    """
    if printer in PRINTERS:
    	process = subprocess.Popen(['perl /afs/athena.mit.edu/contrib/consult/arch/common/bin/hpcheck.pl ' + printer], shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=False)
    	parsed_data = _parse_raw_data(process.communicate()[0], printer)
        result = _process_dict(parsed_data)
        return result
    else:
        return {}
    
def _parse_raw_data(data, printer):
    """
    Param
        data: raw string from perl script
    Return
        output: dict of printer info { (fieldName, order): value, ect}   
    """
    lines = data.split("\n")[:-1]
    output = {}
    for i in xrange(len(lines)):
        lines[i] = lines[i].replace(":", "|", 1).replace("\t", "")
        split = lines[i].split("|")
        if len(split) >=2 :
            #output[(get_key(split[0]), i)] = split[1].strip()
            output[_get_key(split[0])] = split[1].strip()
    output[_get_key('printerName')] = printer
    return output
    
def _get_key(field):
    return field.replace("(", "").replace(" ", "").replace(")", "")

def _process_dict(data):
    """
    Add in the extra fields
    """
    new_dict = {}
    for key in data.keys():
	new_dict['name'] = data['printerName']
        #new_dict[key] = data[key]

    #FIGURE OUT AND UPDATE PRINTER STATUS
    status = BUSY_STATUS
    error_msg = ""
    if "FrontPanelMessage" in data:
        if data["FrontPanelMessage"].lower() in READY_MESSAGES:
            status = READY_STATUS
        elif "error" in data["FrontPanelMessage"].lower():
            status = ERROR_STATUS
            error_msg = "general error"
    
    if "TonerStatus" in data:
        if data["TonerStatus"].find("2") != -1:
            status = ERROR_STATUS
            error_msg = "Toner Error"
        #if len(new_dict["TonerStatus"]) > 4:
            #new_dict["TonerStatus"] = new_dict["TonerStatus"][4:]

    if "PaperStatus" in data:
        if data["PaperStatus"].find("2") != -1:
            status = ERROR_STATUS
            error_msg = "Paper Status Error"
        elif data["PaperStatus"].find("1") != -1:
            status = ERROR_STATUS
            error_msg = "Out of Paper"
        #if len(new_dict["PaperStatus"]) > 4:
            #new_dict["PaperStatus"] = new_dict["PaperStatus"][4:]

    if "PaperJamStatus" in data:
        if data["PaperJamStatus"].find("1") != -1:
            status = ERROR_STATUS
            error_msg = "Paper Jam"
        #if len(new_dict["PaperJamStatus"]) > 4:
            #new_dict["PaperJamStatus"] = new_dict["PaperJamStatus"][4:]

    new_dict["status"] = status
    new_dict["error_msg"] = error_msg
    new_dict["location"] = PRINTERS[new_dict["name"]][0]
    new_dict["building_name"] = PRINTERS[new_dict["name"]][1]
    new_dict["latitude"] = PRINTERS[new_dict["name"]][2]
    new_dict["longitude"] = PRINTERS[new_dict["name"]][3]
    new_dict["atResidence"] = PRINTERS[new_dict["name"]][4]
    return new_dict

def get_distance(lat1, long1, lat2, long2):
    """
    Returns the euclidean distance between two lat/long points
    TODO: make more accurate?
    """
    x = 69.1*(lat2 - lat1)
    y = 69.1*(long2 - long1) * math.cos(lat1/57.3)
    dist = math.sqrt(x*x + y*y)
    return dist
