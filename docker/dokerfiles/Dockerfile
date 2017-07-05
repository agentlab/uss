FROM java:8	

ARG home_dir=/home/edt/uss

RUN mkdir -p $home_dir
WORKDIR $home_dir

EXPOSE 8080 

COPY . $home_dir

RUN chmod -R 777 $home_dir

CMD ./eclipse
