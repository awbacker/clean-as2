#!/bin/sh

_temp="temp"
_home="home"

# main function, called on the last line of the script.  this avoids function delcaration order issues
# accepts all command line args here
function main() {
    partner1=${1:-server-a}      # default value "server-a"
    partner2=${2:-server-b}
    pwd=${3:-8675309}

    wipeDirectory "temp"
    wipeDirectory "home"

    echo ""
    echo "-- CREATING '${partner1}' FILES "
    echo "--------------------------------------------------------------------"
    createKeysAndCertificates ${partner1} ${pwd}

        echo ""
    echo "-- CREATING '${partner2}' FILES "
    echo "--------------------------------------------------------------------"
    createKeysAndCertificates ${partner2} ${pwd}

    echo ""
    echo "-- CREATING HOME DIRECTORIES & CONFIG FILES"
    echo "--------------------------------------------------------------------"
    createHome ${partner1} ${pwd} 11080 11081 ${partner2} 11090
    createHome ${partner2} ${pwd} 11090 11091 ${partner1} 11080
}

function wipeDirectory() {
    if [ -d ${1} ]; then
        echo "    deleting directory /${1}"
        rm -rf ${1};
    fi
    mkdir ${1}
}

function createHome() {
    partner1=$1
    password=$2
    filePort=$3
    mdnPort=$4
    partner2=$5
    partnerFilePort=$6
    startupFile="${_home}/run-${partner1}.sh"
    partnerHome="${_home}/${partner1}"
    configFile="${partnerHome}/config.json"

    echo "    creating ${partnerHome}"
    mkdir ${partnerHome}/
    mkdir ${partnerHome}/certs

    echo "     - copying certificates to ${partnerHome}/certs/"
    cp ${_temp}/${partner1}.p12 ${partnerHome}/certs/
    cp ${_temp}/${partner2}.crt ${partnerHome}/certs/

    echo "     - create server config : ${configFile}"
    cat as2config.template | sed \
        -e "s:##name##:$partner1:g" \
        -e "s:##password##:$password:g" \
        -e "s:##file_port##:$filePort:g" \
        -e "s:##mdn_port##:$mdnPort:g" \
        -e "s:##partner##:$partner2:g" \
        -e "s:##partner_mdn_port##:$partnerFilePort:g" \
        > ${configFile}

    echo "     - create startup script ${startupFile}"
    cat run-server.sh.template | sed \
        -e "s:##partner##:$partner1:g" \
        > ${startupFile}

    chmod +x ${startupFile}
}

function createKeysAndCertificates() {
    partnerName=$1
    password=$2


    echo "    writing configuration file (pwd=${password})"
    writeConfigFile ${partnerName}

    echo "    creating key (.key), certificate (.cer), and PEM file with both"
    createKeyCertAndPem ${partnerName} ${password}

    echo "    signing certificate (.cer + .csr => crt)"
    signCert ${partnerName} ${pwd}

    echo "    creating p12 file from .PEM file"
    openssl pkcs12 \
        -export -out ${_temp}/${partnerName}.p12 -passout pass:${pwd} \
        -in ${_temp}/${partnerName}.pem -passin pass:${pwd} \
        -name "${partnerName}"

    echo "    finished <${partnerName}.p12, ${partnerName}.crt>"
}

function createKeyCertAndPem() {
    baseName="${_temp}/${1}"
    openssl genrsa -des3 -out ${baseName}.key -passout pass:${2} 1024 2>${_temp}/${1}.log
    openssl req -config ${baseName}.config \
            -new -x509 -nodes -sha1 -days 365 \
            -passin pass:${2} \
            -key ${baseName}.key \
            -out ${baseName}.cer
    cat ${baseName}.key ${baseName}.cer > ${baseName}.pem
}

function signCert() {
    baseName="${_temp}/${1}"
    # create the certificate signing requst (CSR), sign the cer file, and ouput the the signed cert
    openssl req \
        -config  ${baseName}.config \
        -new \
        -key     ${baseName}.key -passin pass:${2} \
        -out     ${baseName}.csr \
        2>${baseName}.log

    openssl x509 -req -days 3650 \
        -in      ${baseName}.csr \
        -signkey ${baseName}.key -passin pass:${pwd} \
        -out     ${baseName}.crt \
        2>${baseName}.log

}

function writeConfigFile() {
    name="${1}"

# We will use this template to create a SSL config file for each partner, edit with care
configFile="
[req]
default_bits           = 1024
default_keyfile        = $name
distinguished_name     = req_distinguished_name
attributes             = req_attributes
prompt                 = no
output_password        = $pwd

[req_distinguished_name]
C                      = cn
ST                     = shanghai
L                      = luwan district
O                      = powere2e
OU                     = it-dev
CN                     = $name
emailAddress           = $name@corp.com

[req_attributes]
challengePassword      = $pwd
"
# end config file block

    echo "$configFile" | sed -e "s:##partner##:$1:g" > ${_temp}/${1}.config
}

main "$@"