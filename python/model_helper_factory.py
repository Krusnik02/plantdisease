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
from keras.utils.vis_utils import plot_model
import application as appl


class model_factory():
    # constructor
    def __init__(self, shape, features_len):
        # store values
        self.shape = shape
        self.features_len = features_len
        self.features_transformed_len = 128


    # нейросеть 2
    def __create_cnn_my_simple(self):
        model = models.Sequential()
        model.description = "CNNSimple"

        # Adds a densely-connected layer with 64 units to the model:
        model.add(layers.Conv2D(64, (3, 3), activation='relu', input_shape=self.shape))
        model.add(layers.MaxPooling2D(pool_size=(2, 2)))
        # Add another:
        model.add(layers.Conv2D(64, (3, 3), activation='relu'))
        model.add(layers.MaxPooling2D(pool_size=(2, 2)))

        return model


    # нейросеть 1
    def __create_cnn_my_cnn1(self):
        model = models.Sequential()
        model.description = "CNN"
        model.add(layers.BatchNormalization(input_shape=self.shape))
        model.add(layers.Conv2D(32, (3, 3), padding='same', activation='relu'))
        model.add(layers.Conv2D(32, (3, 3), padding='same', activation='relu'))
        model.add(layers.Conv2D(32, (3, 3), padding='same', activation='relu'))
        model.add(layers.MaxPooling2D(pool_size=(2, 2)))
        #model.add(layers.Dropout(0.25))

        model.add(layers.BatchNormalization())
        model.add(layers.Conv2D(64, (3, 3), padding='same', activation='relu'))
        model.add(layers.Conv2D(64, (3, 3), padding='same', activation='relu'))
        model.add(layers.Conv2D(64, (3, 3), padding='same', activation='relu'))
        model.add(layers.MaxPooling2D(pool_size=(2, 2)))
        #model.add(layers.Dropout(0.25))

        return model


    def __create_cnn_mnist_indus(self):
        model = models.Sequential()
        model.description = "Indus_model"
        #model.add(layers.BatchNormalization(input_shape=self.shape))
        model.add(layers.Conv2D(filters=64, kernel_size=3, strides=1,
                         padding='same', activation='relu', input_shape=self.shape))
        # 28*28*64
        model.add(layers.MaxPooling2D(pool_size=2, strides=2, padding='same'))
        # 14*14*64

        model.add(layers.Conv2D(filters=128, kernel_size=3, strides=1,
                         padding='same', activation='relu'))
        # 14*14*128
        model.add(layers.MaxPooling2D(pool_size=2, strides=2, padding='same'))
        # 7*7*128

        model.add(layers.Conv2D(filters=256, kernel_size=3, strides=1,
                         padding='same', activation='relu'))
        # 7*7*256
        model.add(layers.MaxPooling2D(pool_size=2, strides=2, padding='same'))
        # 4*4*256

        return model

    def __create_concat_input(self):
        # create inputs
        img_input = layers.Input(self.shape)
        label_input = layers.Input([self.features_len])

        # reshape
        label_input_repeated_color = layers.RepeatVector(self.shape[2])(label_input)  # color dim
        label_input_repeated_color_flatten = layers.Flatten()(label_input_repeated_color)  # repeat not work with arrays
        label_input_repeated_feature = layers.RepeatVector(self.shape[0])(
            label_input_repeated_color_flatten)  # repeat to make concatination
        label_input_reshape = layers.Reshape((self.features_len, self.shape[0], self.shape[2]))(
            label_input_repeated_feature)  # reshape to image size

        # concat
        concat_layer = layers.concatenate([img_input, label_input_reshape], axis=1)

        #return
        return concat_layer, [img_input, label_input]



    def __create_cnn_mnist_indus_functional_concat(self):
        # creating input
        concat_layer, img_input, label_input = self.__create_concat_input()

        # cnn
        conv1 = layers.Conv2D(filters=64, kernel_size=3, strides=1,
                      padding='same', activation='relu', input_shape=self.shape)(concat_layer)
        mxpooling1 = layers.MaxPooling2D(pool_size=2, strides=2, padding='same')(conv1)

        conv2 = layers.Conv2D(filters=128, kernel_size=3, strides=1,
                              padding='same', activation='relu', input_shape=self.shape)(mxpooling1)
        mxpooling2 = layers.MaxPooling2D(pool_size=2, strides=2, padding='same')(conv2)

        conv3 = layers.Conv2D(filters=256, kernel_size=3, strides=1,
                              padding='same', activation='relu', input_shape=self.shape)(mxpooling2)
        model = layers.MaxPooling2D(pool_size=2, strides=2, padding='same')(conv3)

        return model, [img_input, label_input]


    def __create_cnn_mnist_indus_functional(self):
        img_input = layers.Input(self.shape)

        conv1 = layers.Conv2D(filters=64, kernel_size=3, strides=1,
                      padding='same', activation='relu', input_shape=self.shape)(img_input)
        mxpooling1 = layers.MaxPooling2D(pool_size=2, strides=2, padding='same')(conv1)

        conv2 = layers.Conv2D(filters=128, kernel_size=3, strides=1,
                              padding='same', activation='relu', input_shape=self.shape)(mxpooling1)
        mxpooling2 = layers.MaxPooling2D(pool_size=2, strides=2, padding='same')(conv2)

        conv3 = layers.Conv2D(filters=256, kernel_size=3, strides=1,
                              padding='same', activation='relu', input_shape=self.shape)(mxpooling2)
        model = layers.MaxPooling2D(pool_size=2, strides=2, padding='same')(conv3)

        return model, img_input


    def __create_dence_feateres_functional(self):
        input = layers.Input([self.features_len])
        dence1 = layers.Dense(self.features_transformed_len, activation='relu')(input)
        #droput1 = layers.Dropout(0.5)(dence1)  # no 4 Mobile!
        return dence1, input


    # нейросеть создаём
    def create_cnn(self, net_name, num_classes):
        model_net = ''
        weights = 'imagenet'
        new_net= True
        if net_name == 'ResNet50':
            model_net = applications.ResNet50(weights=weights, include_top=False, input_shape=self.shape)
        elif net_name == 'VGG16':
            model_net = applications.VGG16(weights=weights, include_top=False, input_shape=self.shape)

            # set trainamle
            model_net.trainable = True
            trainable = False
            for layer in model_net.layers:
                if layer.name == 'block2_conv1':
                    trainable = True
                layer.trainable = trainable

            # set trainamle
        elif net_name == 'VGG19':
            model_net = applications.VGG19(weights=weights, include_top=False, input_shape=self.shape)
        elif net_name == 'my_cnn_simple':
            model_net = self.__create_cnn_my_simple()
            new_net = False
        elif net_name == 'my_cnn_1':
            model_net = self.__create_cnn_my_cnn1()
            new_net = False
        elif net_name == 'mnist_indus':
            model_net = self.__create_cnn_mnist_indus()
            new_net = False
        else:
            raise Exception('Сеть не в коде')

        # Строим сеть - добавляем в модель сеть вместо слоя
        model = model_net
        if new_net: # для предобученых сетей
            model = models.Sequential()
            model.description = net_name
            model.add(model_net)

        model.add(layers.Flatten())
        model.add(layers.Dense(1024, activation='relu'))
        model.add(layers.Dropout(0.5))
        model.add(layers.Dense(num_classes, activation='softmax'))
        return model


    # нейросеть создаём
    def create_cnn_features(self, net_name, num_classes):
        model = None
        input = None
        label_model = None
        label_input = None
        weights = 'imagenet'

        if net_name == 'ResNet50_concat':
            concat_layer, input = self.__create_concat_input()
            res_net_50 = applications.ResNet50(weights=weights, include_top=False, input_tensor = concat_layer)
            model = res_net_50.layers[-1].output
        elif net_name == 'ResNet50_concat_after_dence':
            res_net_50 = applications.ResNet50(weights=weights, include_top=False, input_shape=self.shape)
            model = res_net_50.layers[-1].output
            input = res_net_50.input
            label_model, label_input = self.__create_dence_feateres_functional()
        elif net_name == 'VGG16_concat':
            concat_layer, input = self.__create_concat_input()
            res_net_50 = applications.VGG16(weights=weights, include_top=False, input_tensor = concat_layer)
            model = res_net_50.layers[-1].output
        elif net_name == 'VGG16_concat_after_dence':
            res_net_50 = applications.VGG16(weights=weights, include_top=False, input_shape=self.shape)
            model = res_net_50.layers[-1].output
            input = res_net_50.input
            label_model, label_input = self.__create_dence_feateres_functional()
        elif net_name == 'mnist_indus_concat':
            model, input = self.__create_cnn_mnist_indus_functional_concat()
        elif net_name == 'mnist_indus_concat_after_dence':
            model, input = self.__create_cnn_mnist_indus_functional()
            label_model, label_input = self.__create_dence_feateres_functional()
        else:
            raise Exception('Сеть не в коде')

        # Строим сеть - добавляем в модель сеть вместо слоя
        layer = layers.Flatten()(model)

        # конкатинируем здесь
        if label_model is not None:
            layer = layers.concatenate([layer, label_model])

        # продолжаем dence
        dence1 = layers.Dense(1024, activation='relu')(layer)
        droput1 = layers.Dropout(0.5)(dence1) # no 4 Mobile!
        dence_final = layers.Dense(num_classes, activation='softmax')(droput1)

        # создаём финальную модель
        if label_model is None:
            model_final = models.Model(inputs=input, outputs=dence_final, name=net_name)
        else:
            model_final = models.Model(inputs=[input, label_input], outputs=dence_final, name=net_name)


        # plot model & summary
        model_final.summary()
        plot_model(model_final, to_file=appl.PomidorDiseaseApp.temp_path + net_name + '.png', show_shapes=True, show_layer_names=True)


        # return
        return model_final