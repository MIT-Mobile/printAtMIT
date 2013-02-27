This is the backend of the mobile app print@MIT. Computer must be part of the MIT network.

Installing instructions

Step 1: This script depends on perl module Net::SNMP. Install it using 
        cpan Net::SNMP

//Step 2: Install wkhtmltopdf

//sudo wget http://wkhtmltopdf.googlecode.com/files/wkhtmltopdf-0.11.0_rc1-static-amd64.tar.bz2 

Using Development Server:

Change settings.py
    databases
    media_root
    media_url
    static_root
    static_url

run
    python manage.py syncdb
    python manage.py runserver 8080

point browser to http://127.0.0.1:8080/query
press update button to populate database


Deploying to Production
   

