# Python 3.6.0
# tensorflow 1.1.0
# Keras 2.0.4



import os
import model_genetics as gen
import application as app
import gc
from plant import plant
import model_helper_factory as mfac
from keras.utils.vis_utils import plot_model

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
from keras import backend as K
from keras.models import load_model



import warnings
warnings.filterwarnings('ignore')


class ModelHelper:
    def __init__(self, def_shape, optimizer, batch_size, verbose):
        self.shape = def_shape
        self.optimizer = optimizer
        self.batch_size = batch_size
        self.verbose = verbose
        self.model_factory = mfac.model_factory(def_shape, 1)



    def train_model(self, x_data, y_data, net, epochs, tt_split, title = None):
        # оповещение
        print("Обучаем %s длинна выборки %i" % (title, len(x_data)))

        # создаём сеть
        num_classes = int(np.max(y_data)) + 1
        model = self.model_factory.create_cnn(net, num_classes)

        # компилируем
        model.compile(loss="categorical_crossentropy", optimizer=self.optimizer, metrics=["accuracy"])
        #model.summary()

        # готовим данные
        y_data_cat = to_categorical(y_data)
        #x_train, x_test, y_train, y_test = train_test_split(x_data, y_data, test_size=tt_split, shuffle=True)

        # callbacks
        #save_best = ModelCheckpoint(self.data_path + '\\recognize_plants.hdf', save_best_only=True,
        #                            monitor='acc', mode='max')

        # обучаем
        hist = model.fit(x_data, y_data_cat, epochs = epochs,
                         batch_size = self.batch_size, validation_split = tt_split,
                         verbose=self.verbose, callbacks = [])

        # рисуем
        plt.plot(hist.history['acc'], label='train_acc')
        if tt_split is not None:
            plt.plot(hist.history['val_acc'], label='val_acc')
        if title is not None:
            plt.title(title)
        plt.xlabel('Эпоха обучения')
        plt.ylabel('Доля верных ответов')
        plt.legend()
        plt.show()

        # валидация
        #result = model.evaluate(x_test, y_test, verbose=2)
        #print("Accuracy на тестовых данных %.2f" % result[1])

        # возвращяем модель
        return model


    def train_model_generator(self, x_data, y_data, net, epochs, tt_split, title = None):
        # оповещение
        print("Обучаем %s длинна выборки %i" % (title, len(x_data)))

        # создаём сеть
        num_classes = int(np.max(y_data)) + 1
        model = self.model_factory.create_cnn(net, num_classes)

        # компилируем
        model.compile(loss="categorical_crossentropy", optimizer=self.optimizer, metrics=["accuracy"])
        #model.summary()

        # готовим данные
        x_data = np.asarray(x_data)
        y_data_cat = to_categorical(y_data)
        x_train = x_data
        y_train = y_data_cat
        x_test = None
        y_test = None
        if tt_split is not None:
            x_train, x_test, y_train, y_test = train_test_split(x_data, y_data_cat, test_size=tt_split, shuffle=True)


        # callbacks
        #save_best = ModelCheckpoint(self.data_path + '\\recognize_plants.hdf', save_best_only=True,
        #                            monitor='acc', mode='max')

        # создаём генератор
        datagen = ImageDataGenerator(rescale=1. ,
                                     rotation_range=45,
                                     width_shift_range=0.2,
                                     height_shift_range=0.2,
                                     zoom_range=0.20,
                                     horizontal_flip=True,
                                     fill_mode='nearest')

        datagen.fit(x_train)

        #model.summary()

        # обучаем
        hist = model.fit_generator(datagen.flow(x_train, y_train, batch_size=self.batch_size), epochs=epochs,
                                   steps_per_epoch=len(x_train) / self.batch_size, verbose=self.verbose,
                                   callbacks=[])

        # рисуем
        plt.plot(hist.history['acc'], label='train_acc')
        if title is not None:
            plt.title(title)
        plt.xlabel('Эпоха обучения')
        plt.ylabel('Доля верных ответов')
        plt.legend()
        plt.show()

        # валидация
        if tt_split > 0:
            result = model.evaluate(x_test, y_test, verbose=2)
            print("Accuracy на тестовых данных %.2f" % result[1])

        # чисчтим память
        del datagen
        gc.collect()
        #clear_memory()

        # возвращяем модель
        return model


    def train_model_geneticks(self, x_data, y_data, input_shape, title = None):
        # оповещение
        print("Обучаем генетику %s" % title)

        # создаём сеть
        num_classes = int(np.max(y_data)) + 1
        model = gen.optimizon(input_shape, num_classes, 15, 1, 1, self.optimizer, 0.05, 1, 1)

        # готовим данные
        y_data_cat = to_categorical(y_data)

        # обучаем
        model.train(x_data, y_data_cat)


        # рисуем лучшее

        # валидация
        # result = model.evaluate(x_test, y_test, verbose=2)
        # print("Accuracy на тестовых данных %.2f" % result[1])

        # возвращяем модель
        return model


    def train_model_features(self, x_data, y_data, parts, net, epochs, tt_split, title = None):
        # оповещение
        print("Обучаем %s длинна выборки %i" % (title, len(x_data)))

        # создаём сеть
        num_classes = int(np.max(y_data)) + 1
        model = self.model_factory.create_cnn_features(net, num_classes)

        # компилируем
        model.compile(loss="categorical_crossentropy", optimizer=self.optimizer, metrics=["accuracy"])
        #model.summary()
        plot_model(model, show_shapes=True, show_layer_names=True)

        # готовим данные
        y_data_cat = to_categorical(y_data)
        parts_axes = parts
        #parts_axes = np.repeat(parts, len(x_data[0]) * len(x_data[0][0]))
        #parts_axes = parts_axes.reshape((-1, len(x_data[0]), len(x_data[0][0]), 1))
        #x_data_part = np.append(x_data, parts_axes, axis=3)


        # обучаем
        hist = model.fit([x_data, parts_axes], y_data_cat, epochs = epochs,
                         batch_size = self.batch_size, validation_split = tt_split,
                         verbose=self.verbose, callbacks = [])

        # рисуем
        plt.plot(hist.history['acc'], label='train_acc')
        if tt_split is not None:
            plt.plot(hist.history['val_acc'], label='val_acc')
        if title is not None:
            plt.title(title)
        plt.xlabel('Эпоха обучения')
        plt.ylabel('Доля верных ответов')
        plt.legend()
        plt.show()

        # валидация
        #result = model.evaluate(x_test, y_test, verbose=2)
        #print("Accuracy на тестовых данных %.2f" % result[1])

        # возвращяем модель
        return model

    def train_model_features_generator(self, x_data, y_data, features, net, epochs, tt_split, title = None):
        # оповещение
        print("Обучаем %s длинна выборки %i" % (title, len(x_data)))

        # создаём сеть
        num_classes = int(np.max(y_data)) + 1
        model = self.model_factory.create_cnn_features(net, num_classes)

        # компилируем
        model.compile(loss="categorical_crossentropy", optimizer=self.optimizer, metrics=["accuracy"])

        # готовим данные
        x_data = np.asarray(x_data)
        y_data_cat = to_categorical(y_data)
        x_train = x_data
        y_train = y_data_cat
        x_test = None
        y_test = None
        features_train = None
        features_test = None
        if tt_split is not None:
            x_train, x_test, features_train, features_test, y_train, y_test = train_test_split(x_data, features, y_data_cat, test_size=tt_split, shuffle=True)

        # callbacks
        # save_best = ModelCheckpoint(self.data_path + '\\recognize_plants.hdf', save_best_only=True,
        #                            monitor='acc', mode='max')

        # создаём генератор
        datagen = ImageDataGenerator(rescale=1.,
                                     rotation_range=45,
                                     width_shift_range=0.2,
                                     height_shift_range=0.2,
                                     zoom_range=0.20,
                                     horizontal_flip=True,
                                     fill_mode='nearest')

        datagen.fit(x_train)

        # обучаем
        hist = model.fit_generator(datagen.flow([x_train, features_train], y_train, batch_size=self.batch_size), epochs=epochs,
                                   steps_per_epoch=len(x_train) / self.batch_size, verbose=self.verbose,
                                   callbacks=[])

        # рисуем
        plt.plot(hist.history['acc'], label='train_acc')
        if title is not None:
            plt.title(title)
        plt.xlabel('Эпоха обучения')
        plt.ylabel('Доля верных ответов')
        plt.legend()
        plt.show()

        # валидация
        if tt_split > 0:
            result = model.evaluate([x_test, features_test], y_test, verbose=2)
            print("Accuracy на тестовых данных %.2f" % result[1])

        # чисчтим память
        del datagen
        gc.collect()
        # clear_memory()

        # возвращяем модель
        return model


    def export_model(self, model, model_name, model_path):
        # create dirs
        model_path_name = model_path + '/' + model_name
        if not os.path.exists(model_path):
            os.mkdir(model_path)

        # layer names
        input_node_name1 = [model.layers[4].name + '_input']
        input_node_name2 = [model.layers[0].name + '_input']
        output_node_name = model.layers[len(model.layers) - 1].name + "/Softmax"

        # normal save
        model.save(model_path_name + '.h5')

        # writing graph
        tf.io.write_graph(K.get_session().graph_def, model_path, model_name + '_graph.pbtxt')

        # saver save
        saver = tf.compat.v1.train.Saver()
        saver.save(K.get_session(), model_path_name + '.chkp')

        freeze_graph.freeze_graph(model_path_name + '_graph.pbtxt', None,
                                  False, model_path_name + '.chkp', output_node_name,
                                  "save/restore_all", "save/Const:0",
                                  model_path + '/frozen_' + model_name + '.pb', True, "")


        input_graph_def = tf.GraphDef()
        with tf.gfile.Open(model_path + '/frozen_' + model_name + '.pb', "rb") as f:
            input_graph_def.ParseFromString(f.read())

        output_graph_def = optimize_for_inference_lib.optimize_for_inference(
            input_graph_def, [input_node_name1, input_node_name2], [output_node_name],
            tf.float32.as_datatype_enum)

        with tf.gfile.FastGFile(model_path + '/opt_' + model_name + '.pb', "wb") as f:
            f.write(output_graph_def.SerializeToString())

        print("graph saved: %s " % model_name)


    def create_model_path(self, model_path, plant, model_tp, part):
        # create path plant
        path = os.path.join(model_path, plant)
        if not os.path.exists(path):
            os.mkdir(path)

        # create path disease
        if model_tp is not None:
            path = os.path.join(path, model_tp)
            if not os.path.exists(path):
                os.mkdir(path)

        # create path part
        if part is not None:
            path = os.path.join(path, part)
            if not os.path.exists(path):
                os.mkdir(path)

        return path

    def print_model_inputoutput(self, name, model):
        input_node_name = model.layers[0].name + '_input'
        output_node_name = model.layers[len(model.layers) - 1].name + "/Softmax"
        print("Модель: %s input: \'%s\' output: \'%s\'" % (name, input_node_name, output_node_name))


def clear_memory(model = None):
    has_model = model is not None
    tmp_model_name = None
    if model is not None:
        tmp_model_name = app.PomidorDiseaseApp.temp_path + "model.h5"
        model.save(tmp_model_name)
        del model

    gc.collect()
    K.clear_session()
    gc.collect()

    if has_model:
        return load_model(tmp_model_name)
