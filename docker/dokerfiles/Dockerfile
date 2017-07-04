FROM java:8	

ARG home_dir=/home/edt/uss

#ARG UID=1000

#RUN useradd -u $UID -m $UID

#ENV HOME=/home/$UID

#WORKDIR $HOME
#USER $UID
#RUN mkdir -p $HOME

RUN mkdir -p $home_dir
WORKDIR $home_dir


EXPOSE 8080 

COPY ../.. $home_dir

#COPY . $HOME

#CMD ./eclipse
