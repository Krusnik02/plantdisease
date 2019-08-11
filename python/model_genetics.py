import model_helper as mhp

from keras.models import Sequential
from keras.layers import Dense, Conv2D, Flatten, MaxPooling2D, BatchNormalization, Dropout


import random as random
import matplotlib.pyplot as plt




class model_test_result:
    def __init__(self, f, model, history):
        self.f = abs(f)
        self.model = model
        self.history = history

    def report(self):
        # рисуем модель
        self.model.summary()

        # рисуем
        plt.plot(self.history.history['acc'], label='train_acc')
        if 'val_acc' in self.history.history:
            plt.plot(self.history.history['val_acc'], label='val_acc')
        plt.xlabel('Эпоха обучения')
        plt.ylabel('Доля верных ответов')
        plt.legend()
        plt.show()


class optimizon:

    def __init__(self, input_shape, num_classes, batch_size, epochs_gen, epochs_net, optimizer, mutabilnost, gen_population_count, survival_rate):
        # store data
        self.input_shape = input_shape
        self.num_classes = num_classes
        self.epochs_gen = epochs_gen
        self.epochs_net = epochs_net
        self.batch_size = batch_size
        self.verboze = 2
        self.net_loss = "categorical_crossentropy"
        self.net_optimizer = optimizer
        self.gen_population_count = gen_population_count
        self.gen_mut = mutabilnost
        self.survival_rate = survival_rate

        # crate empty class members
        self._gen_popul = []
        self._gen_val = []


    def __createConvNet(self, net):
        model = Sequential()

        makeFirstNormalization = net[0]
        firstConvSize = 2 ** net[1]
        firstConvKernel = net[2]
        makeSecondConv = net[3]
        secondConvSize = 2 ** net[4]
        secondConvKernel = net[5]
        makeMaxPooling = net[6]
        maxPoolingSize = net[7]
        makeSecondNormalization = net[8]
        denseSize = 2 ** net[9]

        if (makeFirstNormalization):
            model.add(BatchNormalization(input_shape=self.input_shape))
            model.add(Conv2D(firstConvSize, (firstConvKernel, firstConvKernel), activation="relu", padding='same', ))
        else:
            model.add(Conv2D(firstConvSize, (firstConvKernel, firstConvKernel), input_shape=self.input_shape, activation="relu",
                             padding='same'))

        if (makeSecondConv):
            model.add(Conv2D(secondConvSize, (secondConvKernel, secondConvKernel), activation="relu"))

        if (makeMaxPooling):
            model.add(MaxPooling2D(pool_size=(maxPoolingSize, maxPoolingSize)))

        if (makeSecondNormalization):
            model.add(BatchNormalization())

        model.add(Flatten())
        model.add(Dense(denseSize, activation="relu"))
        model.add(Dense(self.num_classes, activation="softmax"))

        return model


    def __evaluateNet(self, x_data, y_data, net):
        val = 0
        model = self.__createConvNet(net)
        model.compile(loss=self.net_loss, optimizer=self.net_optimizer, metrics=["accuracy"])
        history = model.fit(x_data,
                            y_data,
                            batch_size=self.batch_size,
                            epochs=self.epochs_net,
                            validation_split=0.2,
                            verbose=self.verboze)
        val = history.history["val_acc"][len(history.history["val_acc"]) - 1]

        model = mhp.clear_memory(model)
        return val, model, history


    def __createRandomNet(self):
        net = []
        net.append(random.randint(0, 1))
        net.append(random.randint(4, 8))
        net.append(random.randint(3, 7))
        net.append(random.randint(0, 1))
        net.append(random.randint(4, 8))
        net.append(random.randint(3, 7))
        net.append(random.randint(0, 1))
        net.append(random.randint(2, 4))
        net.append(random.randint(0, 1))
        net.append(random.randint(5, 9))
        return net


    def train(self, x_data, y_data):
        # параметры сети
        n = 10
        l = 10

        # создаём начальные сети
        self._gen_popul = []
        for i in range(n):
            self._gen_popul.append(self.__createRandomNet())

        # обучаем генетику
        for iter in range(self.epochs_gen):
            # обучаем все сети, добавляем результат в __gen_val
            self._gen_val = []
            gv = []
            for i in range(n):
                f, model, history = self.__evaluateNet(x_data, y_data, self._gen_popul[i])
                test_res = model_test_result(f, model, history)
                self._gen_val.append(test_res)
            sval = sorted(self._gen_val, reverse=True, key=lambda model_res: model_res.f)
            print(iter, " ", sval[0].f, " ", sval[1].f, " ", sval[2].f, " ", sval[3].f, " ", sval[4].f)

            # мудируем - копируем лучшие
            newpopul = []
            for i in range(self.survival_rate):
                index = self._gen_val.index(sval[i])
                newpopul.append(self._gen_popul[index])

            # мудируем - замешиваем
            for i in range(n - self.survival_rate):
                # отбираем 2 случайных
                indexp1 = random.randint(0, self.survival_rate - 1)
                indexp2 = random.randint(0, self.survival_rate - 1)
                botp1 = newpopul[indexp1]
                botp2 = newpopul[indexp2]

                # делаем им корсинговер
                newbot = []
                net4Mut = self.__createRandomNet()
                for j in range(l):
                    x = 0
                    pindex = random.random()
                    if pindex < 0.5:
                        x = botp1[j]
                    else:
                        x = botp2[j]

                    if (random.random() < self.gen_mut):
                        x = net4Mut[j]

                    newbot.append(x)

                newpopul.append(newbot)

            # устанавливаем новую попуЛяцию
            self._gen_popul = newpopul

            # печатаем отчёт о лучшей сети
            sval[0].report()




class optimizon_aug(optimizon):

    def __init__(self, input_shape, num_classes, batch_size, epochs_gen, epochs_net, optimizer, mutabilnost,
                 survival_rate):

        # base class
        super(optimizon_aug, self).__init__(input_shape, num_classes, batch_size, epochs_gen, epochs_net, optimizer,
                                            mutabilnost, survival_rate)

    def __evaluateNet(self, x_data, y_data, net):
        val = 0
        model = self.__createConvNet(net)
        model.compile(loss=self.net_loss, optimizer=self.net_optimizer, metrics=["accuracy"])
        history = model.fit(x_data,
                            y_data,
                            batch_size=self.batch_size,
                            epochs=self.epochs_net,
                            validation_split=0.2,
                            verbose=self.verboze)
        val = history.history["val_acc"][len(history.history["val_acc"]) - 1]
        return val, model, history