#ps1

Write-Host "Running install Jenkins Agent as a Windows Service"
Write-Host "VERSION 0.2.1"

$service_name = "Jenkins Agent Service"
$service_description = "Jenkins Agent Service"
$service_startup_type = "Automatic"
$jenkins_dir = "@@@jenkins_dir@@@"
$jenkins_slave_location = ($jenkins_dir+"\slave.jar")
$jenkins_exe_location = ($jenkins_dir+"\jenkins-slave.exe")
$jenkins_config_location = ($jenkins_dir+"\jenkins-slave.exe.config")
$jenkins_xml_location = ($jenkins_dir+"\jenkins-slave.xml")
$jenkins_service_user = "@@@jenkins_user@@@"
$jenkins_service_password = "@@@jenkins_password@@@"

Write-Host "Adding user SELog"

secedit /export /cfg config.inf
secedit /import /cfg config.inf /db database.sdb
# Add user to the SeServiceLogonRight line. 
# If the user is already there, it is added again, but nothing bad happens, and there is only one copy in the end
(gc config.inf) -replace 'SeServiceLogonRight =',('SeServiceLogonRight = @@@jenkins_user@@@,')|sc config_mod.inf

secedit /configure /db database.sdb /cfg config_mod.inf


Write-Host "Creating Jenkins Directory"
New-Item $jenkins_dir -type directory

Write-Host "Downloading Jenkins slave.jar"
$webclient = New-Object System.Net.WebClient
$webclient.DownloadFile("@@@jenkins_server@@@/jnlpJars/slave.jar", $jenkins_slave_location)
Write-Host "Downloading jenkins-slave.exe files"
$webclient.DownloadFile("@@@jenkins_server@@@/jclouds-jnlp?file=jenkins-slave.exe", $jenkins_exe_location)
$webclient.DownloadFile("@@@jenkins_server@@@/jclouds-jnlp?file=jenkins-slave.exe.config", $jenkins_config_location)
$webclient.DownloadFile("@@@jenkins_server@@@/jclouds-jnlp?file=jenkins-slave.xml", $jenkins_xml_location)

(gc $jenkins_xml_location) -replace '@@server@@','@@@jenkins_server@@@'|sc $jenkins_xml_location
(gc $jenkins_xml_location) -replace '@@slave_id@@','@@@slave_id@@@'|sc $jenkins_xml_location
(gc $jenkins_xml_location) -replace '@@java@@','@@@java@@@'|sc $jenkins_xml_location
(gc $jenkins_xml_location) -replace '@@secret@@','@@@secret@@@'|sc $jenkins_xml_location

$secpasswd = ConvertTo-SecureString $jenkins_service_password -AsPlainText -Force
$mycreds = New-Object System.Management.Automation.PSCredential (".\@@@jenkins_user@@@", $secpasswd)

Write-Host "Creating new service"
New-Service -name $service_name -binaryPathName $jenkins_exe_location -Description $service_description -displayName $service_name -startupType $service_startup_type -credential $mycreds

Write-Host "Starting Service"
Start-Service $service_name

Write-Host "Service initialisation complete"

