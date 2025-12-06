#!/bin/bash

#Atualizar packages
sudo apt update

if ! command -v nginx &> /dev/null
then
  echo "NGINX not found. Installing..."
  sudo apt install -y nginx
else
  echo "NGINX found."
fi

VIDEODIR=./videos

if ! [ -d "$VIDEODIR" ]; then
  echo "The $VIDEODIR directory does not exists. Creating..."
  mkdir -p "$VIDEODIR"
else
  echo "The $VIDEODIR directory already exists."
fi

sudo cp ./nginx.conf /etc/nginx/

if systemctl is-active --quiet nginx; then
    echo "NGINX already running reloading config only."
    sudo nginx -s reload
else
    echo "NGINX starting"
    sudo nginx -t && sudo systemctl restart nginx
fi

echo "NGINX started with success!"