#!/bin/bash
useradd $SSH_USERNAME
echo -e "$SSH_PASSWORD\n$SSH_PASSWORD" | (passwd --stdin $SSH_USERNAME)
su -l $SSH_USERNAME -c "ssh-keygen -t rsa -b 4096 -f ~/.ssh/id_rsa -N ''"
su -l $SSH_USERNAME -c "cp ~/.ssh/id_rsa.pub ~/.ssh/authorized_keys"
su -l $SSH_USERNAME -c "chmod 600 ~/.ssh/authorized_keys"
exec /usr/sbin/sshd -D
