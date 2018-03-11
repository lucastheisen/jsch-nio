FROM centos:centos7

# Inspired by https://github.com/CentOS/CentOS-Dockerfiles
LABEL maintainer="lucastheisen@pastdev.com"

ENV SSH_USERNAME=test
ENV SSH_PASSWORD=test

RUN yum -y update; yum clean all
RUN yum -y install epel-release openssh-server openssh-clients passwd inotify-tools; yum clean all
RUN set -e; \
    mkdir /var/run/sshd; \
    ssh-keygen -t rsa -f etc/ssh/ssh_host_rsa_key -N ''; \
    ssh-keygen -t ecdsa -f etc/ssh/ssh_host_ecdsa_key -N ''; \
    ssh-keygen -t ed25519 -f etc/ssh/ssh_host_ed25519_key -N ''
    
COPY .dockerfile/files/ /

ENTRYPOINT ["/opt/containerland/run.sh"]