#!/bin/bash
if [ ! $(getent group $SSH_USER_GID) ]; then
  echo "Creating group $SSH_USER_NAME"
  groupadd -g $SSH_USER_GID $SSH_USER_NAME
fi

echo "Creating user $SSH_USER_NAME"
useradd -u $SSH_USER_UID -g $SSH_USER_GID $SSH_USER_NAME

echo "Setting password for user $SSH_USER_NAME"
echo -e "$SSH_USER_PASSWORD\n$SSH_USER_PASSWORD" | (passwd --stdin $SSH_USER_NAME)

echo "Configuring .ssh for $SSH_USER_NAME"
su -l $SSH_USER_NAME -c "ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ''"
su -l $SSH_USER_NAME -c "cp ~/.ssh/id_rsa.pub ~/.ssh/authorized_keys"
su -l $SSH_USER_NAME -c "echo \"localhost $(cat /etc/ssh/ssh_host_rsa_key.pub)\" > ~/.ssh/known_hosts"
su -l $SSH_USER_NAME -c "chmod 600 ~/.ssh/authorized_keys ~/.ssh/known_hosts"

echo "Starging sshd..."
exec /usr/sbin/sshd -D
