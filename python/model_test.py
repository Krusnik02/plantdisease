import img_helper as imh


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


# read model
model = models.load_model('E:\\AI\\PlantDisease\\Models\\PlantType\\plant_type.h5')
print('Модель загружена')

# read data
imgs = np.asarray(imh.read_path_general('E:\\AI\\PlantDisease\\Original images\\Pomidor\\Set1', (32, 32, 3)))
print('Загружены %i' % len(imgs))

# Do classification
my_photos_recognition = model.predict(imgs)
my_photos_recognition = np.argmax(my_photos_recognition, axis=1)
print(my_photos_recognition)