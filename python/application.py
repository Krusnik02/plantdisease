import model_helper as mdhp
import os
from plant_types import plant_type
from plant import plant
import random as rnd
import matplotlib.pyplot as plt


import tensorflow as tf
from tensorflow.python.tools import freeze_graph
from tensorflow.python.tools import optimize_for_inference_lib

import numpy as np
import matplotlib.pyplot as plt
from sklearn.model_selection import train_test_split

import keras
import keras.applications as applications
from keras.datasets import mnist
from keras import backend as K
from keras import models
from keras import layers
from keras.callbacks import EarlyStopping, ModelCheckpoint, ReduceLROnPlateau
from keras.utils import to_categorical
from keras.preprocessing.image import ImageDataGenerator





class PomidorDiseaseApp:
    data_path = 'E:\\AI\\PlantDisease\\train_data\\V2\\'
    models_path = 'E:\\AI\\PlantDisease\\models\\'
    temp_path = 'E:\\AI\\Temp\\'
    model_helper = mdhp.ModelHelper((0,0,0), 0,0,0)

    def __init__(self, shape):
        # записываем данные
        self.shape = shape

        # инициализация
        rnd.seed()

        # создаём модель данных
        self.plants = {#'notplant' : plant.plant('notplant', 0, self.shape),
                    'cucumber' : plant(plant_type.cucumber, self.shape),
                    'pomidor' : plant(plant_type.pomidor, self.shape),
                  }

        # check initial dir
        if not os.path.exists(self.models_path):
            os.mkdir(self.models_path)


    # скачать данные
    def load(self):
        # загрузка пустых изображений
        print('loading images:')
        self.plants['pomidor'].read_images(self.data_path)
        self.plants['cucumber'].read_images(self.data_path)
        print('loading images finished')

    def prepare_data_all_plants(self):
        # готовим данные по всем классам
        data = []
        classes = np.arange(0)
        for k, plant in self.plants.items():
            dt, cls = plant.get_train_data_all()
            data += dt
            classes = np.append(classes, cls)

        # return
        return np.asarray(data), np.asarray(classes)

    def train_classify_plant(self, epochs, tt_split):
        x_data, y_data = self.prepare_data_all_plants()
        self.model_plant_type = PomidorDiseaseApp.model_helper.train_model_generator(x_data, y_data, 'mnist_indus', epochs, tt_split, "classify plant")

        #PomidorDiseaseApp.model_helper.train_model_geneticks(x_data, y_data, PomidorDiseaseApp.shape, "classify plant")

        return self.model_plant_type

    def show_rnd_image(self):
        # выбираем случайный фрукт
        rnd_val = rnd.randrange(0, len(self.plants))
        rnd_plant = list(self.plants.items())[rnd_val][1]
        imgs, cat = rnd_plant.get_train_data_all()

        # выбираем случайную картинку
        rnd_img = rnd.randrange(len(imgs))
        img = imgs[rnd_img]

        # рисуем
        plt.imshow(img)
        plt.show()