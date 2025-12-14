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

if ! command -v ffmpeg &> /dev/null
then
  echo "FFmpeg not found. Installing..."
  sudo apt install -y ffmpeg
else
  echo "FFmpeg found."
fi

if ! command -v mvn &> /dev/null; then
  echo "Maven not found. Installing..."
  sudo apt install -y maven
else
  echo "Maven found."
fi

HLS_HD=/var/www/netflixplus/1080p/hls
HLS_SD=/var/www/netflixplus/360p/hls
MP4_HD=/var/www/netflixplus/1080p/mp4
MP4_SD=/var/www/netflixplus/360p/mp4

for dir in "$HLS_HD" "$HLS_SD" "$MP4_HD" "$MP4_SD"; do
  if ! [ -d "$dir" ]; then
    echo "Creating directory $dir..."
    sudo mkdir -p "$dir"
    sudo chown -R "$USER":"$USER" "$dir"
    sudo chmod 755 "$dir"
  else
    echo "Directory $dir already exists."
  fi
done

sudo cp ./nginx.conf /etc/nginx/

if systemctl is-active --quiet nginx; then
    echo "NGINX already running reloading config only."
    sudo nginx -s reload
else
    echo "NGINX starting"
    sudo nginx -t && sudo systemctl restart nginx
fi

echo "NGINX started with success!"

echo "Building backend with Maven..."
mvn clean package || { echo "Maven build failed"; exit 1; }

echo "Moving backend JAR to build/ directory..."
mv target/netflixplusbackend.jar build/netflixplusbackend.jar